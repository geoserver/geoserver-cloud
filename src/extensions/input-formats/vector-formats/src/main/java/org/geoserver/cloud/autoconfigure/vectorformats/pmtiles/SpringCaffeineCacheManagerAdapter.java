/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.vectorformats.pmtiles;

import io.tileverse.cache.CacheManager;
import io.tileverse.cache.CacheStats;
import io.tileverse.cache.CaffeineCache;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.caffeine.CaffeineCacheManager;

/**
 * Adapter that implements {@link io.tileverse.cache.CacheManager} by delegating to Spring's {@link CaffeineCacheManager}.
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
 * <p>The adapter creates caches on-demand using Tileverse's cache builders, then registers them with Spring's
 * {@link CaffeineCacheManager} for unified management.
 *
 * @see PMTilesPluginAutoConfiguration#setUpCacheManager()
 */
public class SpringCaffeineCacheManagerAdapter implements io.tileverse.cache.CacheManager {

    private CaffeineCacheManager springCaffeineCacheManager;

    private final Collection<String> customCacheNames = new CopyOnWriteArrayList<>();

    /**
     * Creates a new adapter wrapping the given Spring cache manager.
     *
     * @param springCaffeineCacheManager the Spring Caffeine cache manager to delegate to
     */
    public SpringCaffeineCacheManagerAdapter(CaffeineCacheManager springCaffeineCacheManager) {
        this.springCaffeineCacheManager = springCaffeineCacheManager;
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
        return List.copyOf(customCacheNames);
    }

    /**
     * Returns statistics for all caches managed by this adapter.
     *
     * @return map of cache name to statistics
     */
    @Override
    public Map<String, CacheStats> stats() {
        Map<String, CacheStats> stats = new HashMap<>();
        for (String name : customCacheNames) {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> cache = getExistingNativeCache(name);
            if (cache != null) {
                stats.put(name, CaffeineCache.stats(cache));
            }
        }
        return stats;
    }

    /** Invalidates all entries in all caches managed by this adapter. */
    @Override
    public void invalidateAll() {
        for (String cacheName : customCacheNames) {
            springCaffeineCacheManager.getCache(cacheName).invalidate();
        }
    }

    /**
     * Gets or creates a cache with the given identifier.
     *
     * <p>If a cache with the given name already exists in Spring's cache manager, it is wrapped and returned. Otherwise,
     * the provided builder is used to create a new Caffeine cache, which is then registered with Spring's cache manager
     * before being returned.
     *
     * @param cacheIdentifier unique name for the cache
     * @param builder supplier that creates the cache if it doesn't exist
     * @return the cache instance
     */
    @Override
    public <K, V, C extends io.tileverse.cache.Cache<K, V>> C getCache(
            @NonNull String cacheIdentifier, @NonNull Supplier<C> builder) {

        com.github.benmanes.caffeine.cache.Cache<K, V> caffeineCache = getExistingNativeCache(cacheIdentifier);
        if (caffeineCache == null) {
            caffeineCache = buildNativeCache(builder);
            registerCustomCache(cacheIdentifier, caffeineCache);
        }
        @SuppressWarnings("unchecked")
        C cache = (C) new io.tileverse.cache.CaffeineCache<>(caffeineCache);
        return cache;
    }

    @SuppressWarnings("unchecked")
    private <K, V> com.github.benmanes.caffeine.cache.Cache<K, V> getExistingNativeCache(String cacheIdentifier) {
        Collection<String> cacheNames = springCaffeineCacheManager.getCacheNames();
        if (cacheNames.contains(cacheIdentifier)) {
            org.springframework.cache.Cache springCache = springCaffeineCacheManager.getCache(cacheIdentifier);
            return (com.github.benmanes.caffeine.cache.Cache<K, V>) springCache.getNativeCache();
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <K, V> com.github.benmanes.caffeine.cache.Cache<K, V> buildNativeCache(
            @NonNull Supplier<? extends io.tileverse.cache.Cache<K, V>> builder) {

        io.tileverse.cache.CaffeineCache<K, V> tileverseCache = (io.tileverse.cache.CaffeineCache) builder.get();
        return (com.github.benmanes.caffeine.cache.Cache<K, V>) tileverseCache.getNativeCache();
    }

    @SuppressWarnings("unchecked")
    private <K, V> void registerCustomCache(
            String cacheIdentifier, com.github.benmanes.caffeine.cache.Cache<K, V> caffeineCache) {
        com.github.benmanes.caffeine.cache.Cache<Object, Object> cache =
                (com.github.benmanes.caffeine.cache.Cache<Object, Object>) caffeineCache;
        springCaffeineCacheManager.registerCustomCache(cacheIdentifier, cache);
        customCacheNames.add(cacheIdentifier);
    }
}
