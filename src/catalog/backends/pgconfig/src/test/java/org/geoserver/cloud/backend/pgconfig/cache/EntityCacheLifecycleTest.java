/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.List;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup.Miss;
import org.geoserver.config.GeoServer;
import org.junit.jupiter.api.Test;

class EntityCacheLifecycleTest {

    private static final long ONE_MIB = 1024 * 1024;
    private static final CacheKey KEY = CacheKey.entity(LAYER, "layer-1");
    private static final CacheKey UNKNOWN_KEY = CacheKey.entity(LAYER, "unknown");
    private static final byte[] JSON = "{}".getBytes(UTF_8);

    private final EntityCache catalogCache = newCache("gs-catalog");
    private final EntityCache configCache = newCache("gs-config");
    private final List<EntityCache> caches = List.of(catalogCache, configCache);
    private final EntityCacheLifecycle lifecycle = new EntityCacheLifecycle(caches);
    private final GeoServer geoServer = mock(GeoServer.class);

    // a suspended cache keeps the entries stored before: the suspension empties it
    @Test
    void beforeReloadSuspends() {
        putEntries();

        lifecycle.beforeReload();

        assertThat(caches)
                .allMatch(EntityCache::isSuspended, "suspended")
                .allMatch(EntityCacheLifecycleTest::isEmpty, "empty");
    }

    @Test
    void beforeReinitializeSuspends() {
        putEntries();

        lifecycle.beforeReinitialize(geoServer);

        assertThat(caches)
                .allMatch(EntityCache::isSuspended, "suspended")
                .allMatch(EntityCacheLifecycleTest::isEmpty, "empty");
    }

    @Test
    void initializeMarksInitialized() {
        lifecycle.beforeReinitialize(geoServer);

        lifecycle.initialize(geoServer);

        putMisses();
        assertThat(caches)
                .noneMatch(EntityCache::isSuspended, "suspended")
                .allMatch(EntityCacheLifecycleTest::holdsMiss, "holding a miss");
    }

    // at startup the caches are not suspended, and keep the objects read while the configuration loads
    @Test
    void initializeClears() {
        putEntries();

        lifecycle.initialize(geoServer);

        assertThat(caches).allMatch(EntityCacheLifecycleTest::isEmpty, "empty");
    }

    // the end of a reload, failed or not, and of a reload copying another catalog without reinitializing
    @Test
    void onReloadResumes() {
        lifecycle.beforeReload();

        lifecycle.onReload();

        putMisses();
        assertThat(caches)
                .noneMatch(EntityCache::isSuspended, "suspended")
                .allMatch(EntityCacheLifecycleTest::holdsMiss, "holding a miss");
    }

    @Test
    void resetClears() {
        putEntries();

        lifecycle.onReset();

        assertThat(caches)
                .noneMatch(EntityCache::isSuspended, "suspended")
                .allMatch(EntityCacheLifecycleTest::isEmpty, "empty");
    }

    @Test
    void disposeClears() {
        putEntries();

        lifecycle.onDispose();

        assertThat(caches).allMatch(EntityCacheLifecycleTest::isEmpty, "empty");
    }

    private void putEntries() {
        for (EntityCache cache : caches) {
            cache.put(KEY, JSON, cache.epoch());
        }
    }

    // a cache keeps no misses until it is initialized
    private void putMisses() {
        for (EntityCache cache : caches) {
            cache.putMiss(UNKNOWN_KEY, cache.epoch());
        }
    }

    // read without a lookup: a lookup answers Absent while the cache is suspended
    private static boolean isEmpty(EntityCache cache) {
        return cache.caffeine().asMap().isEmpty();
    }

    private static boolean holdsMiss(EntityCache cache) {
        return cache.lookup(UNKNOWN_KEY) instanceof Miss;
    }

    private static EntityCache newCache(String name) {
        return new EntityCache(name, ONE_MIB, Duration.ofSeconds(30), new NoopCodec());
    }
}
