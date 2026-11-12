/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Policy;

/**
 * Reads the weight of an {@link EntityCache} for the tests of other packages. The Caffeine cache of an entity cache is
 * package-private, and this class shares its package.
 */
public final class EntityCacheWeight {

    private EntityCacheWeight() {
        // static methods only
    }

    /** The weight of the entries of the cache, in bytes, once the cache has applied its pending writes. */
    public static long of(EntityCache cache) {
        cache.cleanUp();
        Cache<CacheKey, byte[]> caffeine = cache.caffeine();
        Policy.Eviction<CacheKey, byte[]> eviction =
                caffeine.policy().eviction().orElseThrow();
        return eviction.weightedSize().orElseThrow();
    }
}
