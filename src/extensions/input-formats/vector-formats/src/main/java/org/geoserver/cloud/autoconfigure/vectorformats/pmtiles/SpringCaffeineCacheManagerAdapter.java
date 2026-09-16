/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.vectorformats.pmtiles;

import io.tileverse.cache.Cache;
import io.tileverse.cache.CacheManager;
import io.tileverse.cache.CacheStats;
import io.tileverse.cache.CaffeineCache;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.caffeine.CaffeineCacheManager;

/**
 * Adapter that implements {@link io.tileverse.cache.CacheManager} by delegating to Spring's
 * {@link CaffeineCacheManager}.
 *
 * <p>This adapter is {@link CacheManager#setDefault(CacheManager) set as the default} cache manager for PMTiles and
 * RangeReaders, allowing Tileverse-managed caches to be exposed through Spring Actuator endpoints:
 *
 * <ul>
 *   <li>{@code /actuator/caches} - Cache listing and management
 *   <li>{@code /actuator/metrics/cache.*} - Cache metrics (hits, misses, evictions, etc.)
 *   <li>{@code /actuator/prometheus} - Prometheus-format metrics export
 * </ul>
 *
 * <p>The adapter creates caches on-demand using Tileverse's cache builders and holds on to what each builder returned.
 * The native Caffeine cache behind it is registered with Spring's {@link CaffeineCacheManager} for unified management.
 *
 * @see "PMTilesPluginAutoConfiguration#setUpCacheManager()"
 */
public class SpringCaffeineCacheManagerAdapter implements CacheManager {

    private CaffeineCacheManager springCaffeineCacheManager;

    private final ConcurrentMap<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    /**
     * Creates a new adapter wrapping the given Spring cache manager.
     *
     * @param springCaffeineCacheManager the Spring Caffeine cache manager to delegate to
     */
    public SpringCaffeineCacheManagerAdapter(CaffeineCacheManager springCaffeineCacheManager) {
        this.springCaffeineCacheManager = springCaffeineCacheManager;
    }

    /**
     * Gets or creates a cache with the given identifier.
     *
     * <p>The builder creates the cache once per identifier, and every later call for that identifier returns that same
     * instance. A caller with its own cache type therefore gets its own type back, along with whatever the type holds
     * beyond the native Caffeine cache, such as the asynchronous view that entries are loaded through.
     *
     * @param cacheIdentifier unique name for the cache
     * @param builder supplier that creates the cache if it doesn't exist
     * @return the cache instance
     */
    @Override
    public <K, V, C extends Cache<K, V>> C getCache(@NonNull String cacheIdentifier, @NonNull Supplier<C> builder) {

        Cache<?, ?> cache = caches.computeIfAbsent(cacheIdentifier, name -> buildAndRegister(name, builder));

        @SuppressWarnings("unchecked")
        C typed = (C) cache;
        return typed;
    }

    /**
     * Creates the cache and registers its native Caffeine cache with Spring, where the actuator endpoints read its
     * statistics and its contents.
     */
    private Cache<?, ?> buildAndRegister(String cacheIdentifier, Supplier<? extends Cache<?, ?>> builder) {
        Cache<?, ?> cache = builder.get();
        CaffeineCache<?, ?> caffeineCache = (CaffeineCache<?, ?>) cache;
        springCaffeineCacheManager.registerCustomCache(cacheIdentifier, caffeineCache.getNativeCache());
        return cache;
    }

    /**
     * Returns the names of caches created through this adapter.
     *
     * <p>Note: This only returns caches created by Tileverse/PMTiles, not all caches in Spring's cache manager.
     *
     * @return collection of cache names managed by this adapter
     */
    @Override
    public Collection<String> getCacheNames() {
        return List.copyOf(caches.keySet());
    }

    /**
     * Returns statistics for all caches managed by this adapter.
     *
     * @return map of cache name to statistics
     */
    @Override
    public Map<String, CacheStats> stats() {
        Map<String, CacheStats> stats = new HashMap<>();
        caches.forEach((name, cache) -> stats.put(name, cache.stats()));
        return stats;
    }

    /** Invalidates all entries in all caches managed by this adapter. */
    @Override
    public void invalidateAll() {
        caches.values().forEach(Cache::invalidateAll);
    }
}
