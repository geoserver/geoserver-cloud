/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.repository;

import com.google.common.base.Stopwatch;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.gwc.event.GeoWebCacheEvent;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;

/**
 * Caching decorator for {@link ResourceStoreTileLayerCatalog} using a provided Spring {@link
 * CacheManager}.
 *
 * <p>Two named {@link Cache caches} are taken from the cache manager, {@code TILE_LAYERS_BY_ID} and
 * {@code TILE_LAYERS_BY_NAME}, in order to to point queries by layer id and name respectively.
 *
 * <p>{@code CachingTileLayerCatalog} listens to {@link TileLayerEvent}s to evict cache entries for
 * modified and removed tile layers.
 *
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.gwc.repository")
public class CachingTileLayerCatalog extends ForwardingTileLayerCatalog {

    private static final String TILE_LAYERS_BY_ID = "TILE_LAYERS_BY_ID";

    private final CacheManager cacheManager;

    Cache idCache;
    final BiMap<String, String> namesById = Maps.synchronizedBiMap(HashBiMap.create());

    public CachingTileLayerCatalog(CacheManager cacheManager, ResourceStoreTileLayerCatalog delegate) {
        super(delegate);
        this.cacheManager = cacheManager;
    }

    @EventListener(TileLayerEvent.class)
    public void onTileLayerEvent(TileLayerEvent event) {
        final String infoId = event.getPublishedId();

        if (event.getEventType() == GeoWebCacheEvent.Type.DELETED) {
            namesById.remove(infoId);
        } else {
            namesById.forcePut(infoId, event.getName());
        }
        if (evict(infoId)) {
            log.debug("Evicted GeoServerTileLayerInfo[{}] upon event {}", infoId, event);
        } else {
            log.trace("Event didn't result in evicting GeoServerTileLayerInfo[{}]: {}", infoId, event);
        }
    }

    @Override
    public Set<String> getLayerIds() {
        return Set.copyOf(this.namesById.keySet());
    }

    @Override
    public Set<String> getLayerNames() {
        return Set.copyOf(this.namesById.values());
    }

    @Override
    public GeoServerTileLayerInfo save(GeoServerTileLayerInfo tl) {
        if (evict(tl.getId())) {
            log.debug("Preemtively evicted GeoServerTileLayerInfo[{}] on save", tl.getId());
        }
        super.save(tl);
        GeoServerTileLayerInfo curr = super.getLayerById(tl.getId());
        return cachePut(curr);
    }

    @Override
    public GeoServerTileLayerInfo delete(@NonNull String id) {
        namesById.remove(id);
        idCache.evictIfPresent(id);
        return super.delete(id);
    }

    private boolean evict(@NonNull String id) {
        // note evictIfPresent ought to be used for guaranteed immediate eviction
        return null != idCache && idCache.evictIfPresent(id);
    }

    @Override
    public synchronized void reset() {
        if (idCache != null) {
            idCache.clear();
            idCache = null;
        }
        this.namesById.clear();
        super.reset();
    }

    @Override
    public void initialize() {
        super.initialize();
        this.idCache = cacheManager.getCache(TILE_LAYERS_BY_ID);
        preLoad();
    }

    /** pre-loading makes a real impact when there are several (like in thousands) of tile layers */
    private void preLoad() {
        log.info("Caching GeoServerTileLayerInfos from " + delegate.getPersistenceLocation());
        Stopwatch sw = Stopwatch.createStarted();
        ResourceStoreTileLayerCatalog store = (ResourceStoreTileLayerCatalog) delegate;
        long count = store.findAll().map(this::cachePut).count();
        log.info("Cached %,d GeoServerTileLayerInfos in %s".formatted(count, sw.stop()));
    }

    private @NonNull GeoServerTileLayerInfo cachePut(@NonNull GeoServerTileLayerInfo info) {
        idCache.put(info.getId(), info);
        namesById.forcePut(info.getId(), info.getName());
        log.debug("cached GeoServerTileLayerInfo[{}]", info.getName());
        return info;
    }

    @Override
    public String getLayerId(@NonNull String layerName) {
        return this.namesById.inverse().get(layerName);
    }

    @Override
    public String getLayerName(@NonNull String layerId) {
        String found = this.namesById.get(layerId);
        if (null == found) {
            getLayerById(layerId);
            found = this.namesById.get(layerId);
        }
        return found;
    }

    @Override
    public GeoServerTileLayerInfo getLayerById(@NonNull String id) {
        try {
            var tl = idCache.get(id, () -> loadLayerById(id));
            namesById.forcePut(tl.getId(), tl.getName());
            return tl;
        } catch (Cache.ValueRetrievalException e) {
            if (e.getCause() instanceof NoSuchElementException) return null;
            throw e;
        }
    }

    @Override
    public GeoServerTileLayerInfo getLayerByName(@NonNull String layerName) {
        String id = this.namesById.inverse().get(layerName);
        if (null == id) {
            try {
                var tl = loadLayerByName(layerName);
                namesById.forcePut(tl.getId(), tl.getName());
                return tl;
            } catch (NoSuchElementException e) {
                return null;
            }
        }
        return loadLayerById(id);
    }

    /**
     * loader function for {@link #getLayerById(String)}
     *
     * @throws NoSuchElementException to prevent caching a {@code null} value
     */
    private GeoServerTileLayerInfo loadLayerById(String id) {
        return Optional.ofNullable(super.getLayerById(id)).orElseThrow(() -> new NoSuchElementException(id));
    }

    /**
     * loader function for {@link #getLayerByName(String)}
     *
     * @throws NoSuchElementException to prevent caching a {@code null} value
     */
    private GeoServerTileLayerInfo loadLayerByName(String name) {
        return Optional.ofNullable(super.getLayerByName(name)).orElseThrow(() -> new NoSuchElementException(name));
    }
}
