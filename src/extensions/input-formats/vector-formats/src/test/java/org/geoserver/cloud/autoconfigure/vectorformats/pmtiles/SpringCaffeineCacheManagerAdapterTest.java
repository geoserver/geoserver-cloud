/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.vectorformats.pmtiles;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.tileverse.cache.Cache;
import io.tileverse.cache.CacheStats;
import io.tileverse.cache.CaffeineCache;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCacheManager;

/**
 * Test suite for {@link SpringCaffeineCacheManagerAdapter}.
 *
 * @since 2.28
 */
class SpringCaffeineCacheManagerAdapterTest {

    private static final String CACHE_NAME = "test-cache";

    private CaffeineCacheManager springCacheManager;

    private SpringCaffeineCacheManagerAdapter adapter;

    @BeforeEach
    void setUp() {
        springCacheManager = new CaffeineCacheManager();
        adapter = new SpringCaffeineCacheManagerAdapter(springCacheManager);
    }

    @Test
    void getCacheReturnsTheCacheBuiltBySupplier() {
        AsyncBackedCache built = AsyncBackedCache.create();

        AsyncBackedCache returned = adapter.getCache(CACHE_NAME, () -> built);

        assertThat(returned).isSameAs(built);
    }

    @Test
    void getCacheBuildsOneCachePerName() {
        AsyncBackedCache first = AsyncBackedCache.create();
        AsyncBackedCache second = AsyncBackedCache.create();
        AsyncBackedCache initial = adapter.getCache(CACHE_NAME, () -> first);

        AsyncBackedCache returned = adapter.getCache(CACHE_NAME, () -> second);

        assertThat(initial).isSameAs(first);
        assertThat(returned).isSameAs(first);
    }

    @Test
    void getCacheRegistersTheNativeCacheWithSpring() {
        AsyncBackedCache built = AsyncBackedCache.create();

        adapter.getCache(CACHE_NAME, supplierOf(built));

        org.springframework.cache.Cache springCache = springCacheManager.getCache(CACHE_NAME);
        assertThat(springCache).isNotNull();
        assertThat(springCache.getNativeCache()).isSameAs(built.getNativeCache());
        assertThat(adapter.getCacheNames()).containsExactly(CACHE_NAME);
    }

    @Test
    void springReportsEntriesLoadedThroughTheAsynchronousCache() {
        AsyncBackedCache built = AsyncBackedCache.create();
        adapter.getCache(CACHE_NAME, supplierOf(built));

        load(built, "key", "value");

        assertThat(nativeCacheRegisteredWithSpring().estimatedSize()).isEqualTo(1L);
        Map<String, CacheStats> stats = adapter.stats();
        assertThat(stats).containsKey(CACHE_NAME);
        assertThat(stats.get(CACHE_NAME).entryCount()).isEqualTo(1L);
    }

    @Test
    void invalidateAllClearsTheCacheBehindTheAsynchronousView() {
        AsyncBackedCache built = AsyncBackedCache.create();
        adapter.getCache(CACHE_NAME, supplierOf(built));
        load(built, "key", "value");

        adapter.invalidateAll();

        assertThat(built.getIfPresent("key")).isNull();
    }

    /** A builder typed to the {@link Cache} interface, as a caller with no specialized cache type declares it. */
    private static Supplier<Cache<String, String>> supplierOf(AsyncBackedCache cache) {
        return () -> cache;
    }

    private static void load(AsyncBackedCache cache, String key, String value) {
        cache.asyncCache()
                .get(key, (k, executor) -> CompletableFuture.completedFuture(value))
                .join();
    }

    @SuppressWarnings("unchecked")
    private com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCacheRegisteredWithSpring() {
        org.springframework.cache.Cache springCache = springCacheManager.getCache(CACHE_NAME);
        return (com.github.benmanes.caffeine.cache.Cache<Object, Object>) springCache.getNativeCache();
    }

    /**
     * A cache shaped like parquetry's footer metadata cache: a {@link CaffeineCache} over the synchronous view of an
     * {@link AsyncCache}, keeping the asynchronous handle that its callers load entries through. The handle is
     * reachable only from this type, which makes the cache usable at all only when the manager hands back the instance
     * built for it.
     */
    private static final class AsyncBackedCache extends CaffeineCache<String, String> {

        private final AsyncCache<String, String> asyncCache;

        static AsyncBackedCache create() {
            return new AsyncBackedCache(Caffeine.newBuilder().recordStats().buildAsync());
        }

        private AsyncBackedCache(AsyncCache<String, String> asyncCache) {
            super(asyncCache.synchronous());
            this.asyncCache = asyncCache;
        }

        AsyncCache<String, String> asyncCache() {
            return asyncCache;
        }
    }
}
