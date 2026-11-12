/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Policy;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import lombok.NonNull;

/**
 * Registers the statistics of an {@link EntityCache} with Micrometer: the meters of {@link CaffeineCacheMetrics}, such
 * as {@code cache.gets}, and a {@code cache.weight} gauge in bytes.
 *
 * <p>The meters have the names and tag keys used by Spring Boot for the caches of a cache manager, for the dashboards
 * built on those: {@code cache} and {@code name} hold the name of the cache, and {@code cache.manager} holds
 * {@code pgconfig}.
 *
 * <p>{@code cache.puts} counts the puts that reached the Caffeine cache, including the values dropped by the epoch
 * check. Micrometer reads it from the load count of Caffeine, and the entity cache stores values with a compute of
 * their key, recorded by Caffeine as a load.
 *
 * <p>This binder keeps Micrometer types out of {@link EntityCache}: Spring introspects the methods of a bean class, and
 * fails if one of them names a class missing from the classpath.
 */
public class EntityCacheMetrics implements MeterBinder {

    // The key used by Spring Boot for the caches of a cache manager, written cache_manager by Prometheus
    private static final String CACHE_MANAGER_TAG = "cache.manager";
    private static final String CACHE_MANAGER = "pgconfig";

    private final EntityCache cache;

    public EntityCacheMetrics(@NonNull EntityCache cache) {
        this.cache = cache;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        String name = cache.name();
        Cache<CacheKey, byte[]> caffeine = cache.caffeine();
        Tags tags = Tags.of(CACHE_MANAGER_TAG, CACHE_MANAGER, "name", name);
        CaffeineCacheMetrics.monitor(registry, caffeine, name, tags);
        Tags gaugeTags = tags.and("cache", name);
        registerWeightGauge(registry, caffeine, gaugeTags);
    }

    private static void registerWeightGauge(MeterRegistry registry, Cache<CacheKey, byte[]> caffeine, Tags tags) {
        Gauge.builder("cache.weight", caffeine, EntityCacheMetrics::weightedSize)
                .description("Size of the cached entries: the stored bytes plus an estimate for the keys")
                .baseUnit(BaseUnits.BYTES)
                .tags(tags)
                .register(registry);
    }

    // Present because the entity cache is bounded by weight
    private static long weightedSize(Cache<CacheKey, byte[]> caffeine) {
        Policy<CacheKey, byte[]> policy = caffeine.policy();
        Policy.Eviction<CacheKey, byte[]> eviction = policy.eviction().orElseThrow();
        return eviction.weightedSize().orElseThrow();
    }
}
