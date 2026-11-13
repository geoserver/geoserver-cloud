/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.backend.pgconfig;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.sql.DataSource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.requestcache.RequestCacheScope;
import org.geoserver.cloud.backend.pgconfig.cache.CacheAccess;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.gwc.event.GeoWebCacheEvent;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalWorkspace;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Keeps the tile layers built by the lookups by name of {@link PgconfigTileLayerCatalog}, for the requests of a service
 * instance. A tile request then finds its tile layer without decoding its layer or group, and without building it.
 *
 * <p>The tile layers belong to a generation. After the epoch of the entity cache moves, the first lookup starts a new
 * generation, and drops the tile layers of the older ones. The epoch moves on each eviction: a change of the catalog or
 * of the GWC settings, made on this instance or received from another one, a clear when the event bus reconnects, a
 * reset or a reload. A lookup keeps its tile layer in the generation read before the build. If the epoch moves during
 * the build, the next lookup starts a new generation, and no later lookup returns that tile layer. This holds for a
 * lookup reading entries during their removal: an eviction moves the epoch again once its removal ends. A
 * {@link GeoWebCacheEvent} drops the tile layers too: a change of a gridset or of the GWC defaults changes the tile
 * layers without an eviction.
 *
 * <p>A lookup by name finds the id of the layer or group of the name, then the tile layer of that id. A tile layer
 * looked up by several names, such as its prefixed name and its bare name in a virtual service, is built once. The ids
 * are kept by name and by request context: the workspace isolation of the catalog shows or hides a layer depending on
 * the virtual service of the request and on whether an OWS request runs. The answer "none" is kept as well.
 *
 * <p>A lookup building a tile layer finds the layer or group and builds the tile layer without the cache of the
 * request. A kept tile layer then holds objects of its own, and never objects older than its generation. It holds the
 * layer or group without its {@link ModificationProxy}: the getters of a proxy fill its maps, and the requests sharing
 * the tile layer read plain fields instead. The requests share the kept tile layers and their layers or groups, as with
 * upstream GeoWebCache. The lookups never modify them.
 *
 * <p>The bound counts tile layers, not bytes. A kept tile layer holds a decoded copy of its layer or group, with the
 * members and their references. Measured on a lab catalog, a tile layer takes about 15 KiB for a layer and about 100
 * KiB for a group of 30 layers. A thousand kept groups of that size take about 100 MiB.
 *
 * <p>Lookups build their tile layers and keep nothing without the entity cache, with a bound of 0, while the entity
 * cache is suspended or not initialized, and within a transaction on the database of the catalog.
 */
@Slf4j(topic = "org.geoserver.cloud.gwc.backend.pgconfig")
public class BuiltTileLayerCache {

    /** The name of the cache in the metrics. */
    public static final String NAME = "gwc-tile-layers";

    /**
     * The names kept per tile layer: a tile layer can be looked up by its prefixed name and by its bare name in a
     * virtual service, inside and outside an OWS request.
     */
    private static final int NAMES_PER_TILE_LAYER = 4;

    // Epochs start at 0: the first lookup starts a generation
    private static final long NO_EPOCH = -1;

    private final @Nullable EntityCache entityCache;

    /** When the lookups may keep their tile layers. Null without the entity cache, and with a bound of 0. */
    private final @Nullable CacheAccess access;

    private final Cache<NameKey, Optional<String>> publishedIds;

    private final Cache<IdKey, Optional<GeoServerTileLayer>> tileLayers;

    private final AtomicReference<Generation> current = new AtomicReference<>(new Generation(0, NO_EPOCH));

    /**
     * @param entityCache the entity cache of the catalog, or null without one: lookups then build their tile layers
     * @param dataSource the database of the catalog
     * @param maximumSize the number of tile layers kept, 0 to keep none
     */
    public BuiltTileLayerCache(@Nullable EntityCache entityCache, @NonNull DataSource dataSource, int maximumSize) {
        this.entityCache = entityCache;
        this.tileLayers =
                Caffeine.newBuilder().maximumSize(maximumSize).recordStats().build();
        long maximumNames = (long) NAMES_PER_TILE_LAYER * maximumSize;
        this.publishedIds = Caffeine.newBuilder().maximumSize(maximumNames).build();
        if (null == entityCache || maximumSize == 0) {
            this.access = null;
        } else {
            this.access = new CacheAccess(entityCache, dataSource);
        }
    }

    /**
     * Returns the tile layer of a name: the kept one, or the one built by the builder from the layer or group found by
     * the finder. Empty if the finder finds no layer or group, or if the builder builds no tile layer for it.
     */
    Optional<GeoServerTileLayer> getLayer(
            String layerName,
            Supplier<Optional<PublishedInfo>> finder,
            Function<PublishedInfo, Optional<GeoServerTileLayer>> builder) {
        Optional<Generation> generation = lookupGeneration();
        if (generation.isEmpty()) {
            Optional<PublishedInfo> published = finder.get();
            return published.flatMap(builder);
        }
        return keptOrBuilt(generation.get(), layerName, finder, builder);
    }

    /**
     * Returns the id of the layer or group of a name: the kept one, or the one of the layer or group found by the
     * finder. Empty if the finder finds none.
     */
    Optional<String> findPublishedId(String layerName, Supplier<Optional<PublishedInfo>> finder) {
        Optional<Generation> generation = lookupGeneration();
        if (generation.isEmpty()) {
            Optional<PublishedInfo> published = finder.get();
            return published.map(PublishedInfo::getId);
        }
        NameKey nameKey = NameKey.of(generation.get(), layerName);
        Optional<String> kept = publishedIds.getIfPresent(nameKey);
        if (null != kept) {
            return kept;
        }
        Optional<PublishedInfo> published = RequestCacheScope.withoutCache(() -> findAndKeepId(nameKey, finder));
        return published.map(PublishedInfo::getId);
    }

    /** The generation of the current lookup. Empty if the lookup must keep nothing. */
    private Optional<Generation> lookupGeneration() {
        if (null == entityCache || !keepsTileLayers(entityCache)) {
            return Optional.empty();
        }
        long epoch = entityCache.epoch();
        Generation generation = generationOf(epoch);
        if (generation.epoch() != epoch) {
            // the epoch moved again after this lookup read it: its tile layer may be older than the generation
            return Optional.empty();
        }
        return Optional.of(generation);
    }

    /**
     * Whether the current lookup may keep its tile layer. Not while the entity cache is suspended, nor within a
     * transaction on the database: the lookups then read the database directly. Not before the catalog is initialized
     * either, as the entity cache keeps no misses then, and a lookup may keep the answer "none".
     */
    private boolean keepsTileLayers(EntityCache cache) {
        return null != access && access.inUse() && cache.isInitialized();
    }

    /**
     * Returns the current generation, after starting a new one if the epoch moved since the current one started. A
     * generation started at a later epoch is returned as it is.
     */
    private Generation generationOf(long epoch) {
        Generation generation = current.get();
        while (generation.epoch() < epoch) {
            Generation next = generation.next(epoch);
            if (current.compareAndSet(generation, next)) {
                dropOlderThan(next);
                return next;
            }
            generation = current.get();
        }
        return generation;
    }

    private Optional<GeoServerTileLayer> keptOrBuilt(
            Generation generation,
            String layerName,
            Supplier<Optional<PublishedInfo>> finder,
            Function<PublishedInfo, Optional<GeoServerTileLayer>> builder) {
        NameKey nameKey = NameKey.of(generation, layerName);
        Optional<String> publishedId = publishedIds.getIfPresent(nameKey);
        if (null == publishedId) {
            return buildAndKeep(generation, nameKey, finder, builder);
        }
        if (publishedId.isEmpty()) {
            return Optional.empty();
        }
        IdKey idKey = new IdKey(generation.number(), publishedId.get());
        Optional<GeoServerTileLayer> kept = tileLayers.getIfPresent(idKey);
        if (null == kept) {
            return buildAndKeep(generation, nameKey, finder, builder);
        }
        return kept;
    }

    /**
     * Finds the layer or group of a name and builds its tile layer without the cache of the request, and keeps both
     * answers in the generation. The cache of the request holds objects read before the generation started, and objects
     * changed by a save of the request.
     */
    private Optional<GeoServerTileLayer> buildAndKeep(
            Generation generation,
            NameKey nameKey,
            Supplier<Optional<PublishedInfo>> finder,
            Function<PublishedInfo, Optional<GeoServerTileLayer>> builder) {
        return RequestCacheScope.withoutCache(() -> findThenBuild(generation, nameKey, finder, builder));
    }

    private Optional<GeoServerTileLayer> findThenBuild(
            Generation generation,
            NameKey nameKey,
            Supplier<Optional<PublishedInfo>> finder,
            Function<PublishedInfo, Optional<GeoServerTileLayer>> builder) {
        Optional<PublishedInfo> published = findAndKeepId(nameKey, finder);
        return published.flatMap(found -> buildOnce(generation, found, builder));
    }

    /** Finds the layer or group of a name, and keeps its id for the name, or the answer "none". */
    private Optional<PublishedInfo> findAndKeepId(NameKey nameKey, Supplier<Optional<PublishedInfo>> finder) {
        Optional<PublishedInfo> published = finder.get();
        Optional<String> publishedId = published.map(PublishedInfo::getId);
        publishedIds.put(nameKey, publishedId);
        return published;
    }

    /**
     * Builds the tile layer of a layer or group once per generation: concurrent lookups wait for the first build. The
     * tile layer gets the layer or group without its {@link ModificationProxy}, see the class description.
     */
    private Optional<GeoServerTileLayer> buildOnce(
            Generation generation,
            PublishedInfo published,
            Function<PublishedInfo, Optional<GeoServerTileLayer>> builder) {
        IdKey idKey = new IdKey(generation.number(), published.getId());
        PublishedInfo unwrapped = ModificationProxy.unwrap(published);
        return tileLayers.get(idKey, key -> builder.apply(unwrapped));
    }

    /**
     * Drops the kept tile layers on a change of the GWC configuration, made on this instance or received from another
     * one. A change of a gridset or of the GWC defaults changes the tile layers without an eviction in the entity
     * cache. The listeners applying a change received from another instance, in {@code CloudGwcXmlConfiguration} and
     * {@code CloudGwcConfigPersister}, have the highest precedence: the lookups after the drop build from the changed
     * configuration.
     */
    @EventListener(GeoWebCacheEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void onGeoWebCacheEvent(GeoWebCacheEvent event) {
        clear();
        log.debug("Dropped the built tile layers on {}", event);
    }

    /** Drops the kept tile layers and ids: the next lookups find and build them again. */
    public void clear() {
        Generation next = current.updateAndGet(generation -> generation.next(generation.epoch()));
        dropOlderThan(next);
    }

    /** Drops the ids and tile layers kept by the generations before a new one. */
    private void dropOlderThan(Generation generation) {
        long number = generation.number();
        publishedIds.asMap().keySet().removeIf(key -> key.generation() < number);
        tileLayers.asMap().keySet().removeIf(key -> key.generation() < number);
    }

    /** Returns the Caffeine cache of the tile layers, for the metrics. Callers must not modify it. */
    public Cache<?, ?> caffeine() {
        return tileLayers;
    }

    /** A generation of kept tile layers: its number, and the epoch of the entity cache when it started. */
    private record Generation(long number, long epoch) {

        Generation next(long nextEpoch) {
            return new Generation(number + 1, nextEpoch);
        }
    }

    /**
     * The key of the id of the layer or group of a name. The workspace isolation of the catalog decides what a lookup
     * finds from the workspace of the virtual service and from whether an OWS request runs: the key holds both.
     */
    private record NameKey(long generation, String layerName, @Nullable String localWorkspace, boolean owsRequest) {

        static NameKey of(Generation generation, String layerName) {
            WorkspaceInfo workspace = LocalWorkspace.get();
            String localWorkspace = null == workspace ? null : workspace.getName();
            boolean owsRequest = null != Dispatcher.REQUEST.get();
            return new NameKey(generation.number(), layerName, localWorkspace, owsRequest);
        }
    }

    /** The key of the tile layer of a layer or group. */
    private record IdKey(long generation, String publishedId) {}
}
