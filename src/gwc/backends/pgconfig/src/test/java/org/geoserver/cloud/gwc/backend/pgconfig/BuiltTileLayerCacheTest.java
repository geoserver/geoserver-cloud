/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.backend.pgconfig;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.github.benmanes.caffeine.cache.Cache;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.NoopCodec;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.junit.jupiter.api.Test;

class BuiltTileLayerCacheTest {

    // evicting a key moves the epoch of the entity cache, as a change of the catalog does
    private static final CacheKey CHANGED = CacheKey.entity(ConfigInfoType.LAYER, "changed");

    private static final byte[] JSON = "{\"id\": \"changed\"}".getBytes(UTF_8);

    private final DataSource dataSource = mock(DataSource.class);

    private final EntityCache entityCache = newEntityCache();

    private final AtomicInteger finds = new AtomicInteger();

    @Test
    void epochMoveStartsGeneration() {
        BuiltTileLayerCache cache = new BuiltTileLayerCache(entityCache, dataSource, 10);
        GeoServerTileLayer built = lookUp(cache, "roads");
        GeoServerTileLayer kept = lookUp(cache, "roads");

        entityCache.evict(CHANGED);
        GeoServerTileLayer rebuilt = lookUp(cache, "roads");
        GeoServerTileLayer keptAgain = lookUp(cache, "roads");

        assertThat(kept).isSameAs(built);
        assertThat(rebuilt).isNotSameAs(built);
        assertThat(keptAgain).isSameAs(rebuilt);
    }

    // the build read the catalog before its change
    @Test
    void staleBuildNotServed() {
        BuiltTileLayerCache cache = new BuiltTileLayerCache(entityCache, dataSource, 10);
        Optional<GeoServerTileLayer> stale = cache.getLayer("roads", finder("roads"), this::buildDuringChange);

        GeoServerTileLayer next = lookUp(cache, "roads");

        assertThat(stale).isPresent();
        assertThat(next).isNotSameAs(stale.get());
    }

    // the lookup ran while the eviction removed the entries read by the lookup
    @Test
    void buildDuringEvictionNotServed() {
        BuiltTileLayerCache cache = new BuiltTileLayerCache(entityCache, dataSource, 10);
        entityCache.put(CHANGED, JSON, entityCache.epoch());
        AtomicReference<GeoServerTileLayer> duringEviction = new AtomicReference<>();

        entityCache.evictWhere(key -> lookUpInto(cache, duringEviction));

        GeoServerTileLayer built = duringEviction.get();
        assertThat(built).isNotNull();
        assertThat(lookUp(cache, "roads")).isNotSameAs(built);
    }

    // the build read the GWC configuration before its change
    @Test
    void buildDuringClearNotServed() {
        BuiltTileLayerCache cache = new BuiltTileLayerCache(entityCache, dataSource, 10);
        Supplier<Optional<PublishedInfo>> findDuringClear = () -> findDuringClear(cache, "roads");
        Optional<GeoServerTileLayer> stale = cache.getLayer("roads", findDuringClear, BuiltTileLayerCacheTest::build);

        GeoServerTileLayer next = lookUp(cache, "roads");

        assertThat(stale).isPresent();
        assertThat(next).isNotSameAs(stale.get());
    }

    // another lookup started a newer generation after this lookup read the epoch
    @Test
    void olderEpochKeepsNothing() {
        EntityCache lagging = spy(entityCache);
        BuiltTileLayerCache cache = new BuiltTileLayerCache(lagging, dataSource, 10);
        long readBeforeChange = lagging.epoch();
        lagging.evict(CHANGED);
        lookUp(cache, "rivers");
        doReturn(readBeforeChange).when(lagging).epoch();

        GeoServerTileLayer first = lookUp(cache, "roads");
        GeoServerTileLayer second = lookUp(cache, "roads");

        assertThat(second).isNotSameAs(first);
    }

    @Test
    void boundKeepsAtMost() {
        BuiltTileLayerCache cache = new BuiltTileLayerCache(entityCache, dataSource, 2);

        lookUp(cache, "roads");
        lookUp(cache, "rivers");
        lookUp(cache, "lakes");

        Cache<?, ?> tileLayers = cache.caffeine();
        tileLayers.cleanUp();
        assertThat(tileLayers.estimatedSize()).isLessThanOrEqualTo(2);
    }

    @Test
    void noEntityCacheNoCaching() {
        BuiltTileLayerCache cache = new BuiltTileLayerCache(null, dataSource, 10);

        GeoServerTileLayer first = lookUp(cache, "roads");
        GeoServerTileLayer second = lookUp(cache, "roads");

        assertThat(second).isNotSameAs(first);
    }

    @Test
    void clearDropsAll() {
        BuiltTileLayerCache cache = new BuiltTileLayerCache(entityCache, dataSource, 10);
        GeoServerTileLayer roads = lookUp(cache, "roads");
        GeoServerTileLayer rivers = lookUp(cache, "rivers");

        cache.clear();

        assertThat(lookUp(cache, "roads")).isNotSameAs(roads);
        assertThat(lookUp(cache, "rivers")).isNotSameAs(rivers);
    }

    @Test
    void zeroBoundKeepsNothing() {
        BuiltTileLayerCache cache = new BuiltTileLayerCache(entityCache, dataSource, 0);

        GeoServerTileLayer first = lookUp(cache, "roads");
        GeoServerTileLayer second = lookUp(cache, "roads");

        assertThat(second).isNotSameAs(first);
    }

    // like a miss of the entity cache, a tile layer kept while the catalog loads could miss a change
    @Test
    void uninitializedNoCaching() {
        entityCache.setInitialized(false);
        BuiltTileLayerCache cache = new BuiltTileLayerCache(entityCache, dataSource, 10);

        GeoServerTileLayer first = lookUp(cache, "roads");
        GeoServerTileLayer second = lookUp(cache, "roads");

        assertThat(second).isNotSameAs(first);
    }

    @Test
    void keptNameGivesPublishedId() {
        BuiltTileLayerCache cache = new BuiltTileLayerCache(entityCache, dataSource, 10);
        lookUp(cache, "roads");
        finds.set(0);

        Optional<String> publishedId = cache.findPublishedId("roads", finder("roads"));

        assertThat(publishedId).contains("roads-id");
        assertThat(finds).hasValue(0);
    }

    /** Looks a tile layer up through the cache, and fails the test if the cache finds none. */
    private GeoServerTileLayer lookUp(BuiltTileLayerCache cache, String name) {
        Optional<GeoServerTileLayer> found = cache.getLayer(name, finder(name), BuiltTileLayerCacheTest::build);
        assertThat(found).as(name).isPresent();
        return found.get();
    }

    /** Finds a layer with an id made of the name, as the catalog finds the layer of a tile layer name. */
    private Supplier<Optional<PublishedInfo>> finder(String name) {
        return () -> find(name);
    }

    private Optional<PublishedInfo> find(String name) {
        finds.incrementAndGet();
        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setId(name + "-id");
        return Optional.of(layer);
    }

    /** Looks a tile layer up from an eviction predicate, keeps it for the test, and accepts the key. */
    private boolean lookUpInto(BuiltTileLayerCache cache, AtomicReference<GeoServerTileLayer> found) {
        found.set(lookUp(cache, "roads"));
        return true;
    }

    private Optional<PublishedInfo> findDuringClear(BuiltTileLayerCache cache, String name) {
        cache.clear();
        return find(name);
    }

    private Optional<GeoServerTileLayer> buildDuringChange(PublishedInfo published) {
        entityCache.evict(CHANGED);
        return build(published);
    }

    /** Builds a new tile layer on each call: two lookups answered by one build return the same instance. */
    private static Optional<GeoServerTileLayer> build(PublishedInfo published) {
        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        return Optional.of(tileLayer);
    }

    private static EntityCache newEntityCache() {
        EntityCache cache = new EntityCache("gs-catalog", 1024 * 1024, Duration.ofSeconds(30), new NoopCodec());
        cache.setInitialized(true);
        return cache;
    }
}
