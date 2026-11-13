/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * The access of {@link RequestCachingCatalogFacade} and {@link RequestCachingGeoServerFacade} to the
 * {@link RequestCache} of the request of the thread: their lookups read and fill it, and their writes evict from it.
 * Outside a request, and once the request ended, the lookups run uncached and the evictions do nothing.
 */
final class RequestCacheAccess {

    private RequestCacheAccess() {
        // static methods only
    }

    /**
     * Answers a lookup from the cache of the request, or runs it and caches its answer, a miss included. The lookup
     * runs uncached outside a request, and without a key.
     */
    static <T> @Nullable T memoized(@Nullable Object key, Supplier<@Nullable T> lookup) {
        return memoized(key, lookup, RequestCache::found, RequestCache::put);
    }

    /**
     * Answers a lookup as {@link #memoized(Object, Supplier)} does, and reads and caches the answer with the given
     * functions. A facade uses them when a lookup can be answered from the keys of other lookups.
     */
    static <T> @Nullable T memoized(
            @Nullable Object key, Supplier<@Nullable T> lookup, AnswerReader reader, AnswerWriter writer) {
        RequestCache cache = liveCache();
        if (cache == null || key == null) {
            return lookup.get();
        }
        Optional<Object> cached = reader.cachedAnswer(cache, key);
        if (cached.isPresent()) {
            return valueOf(cached.get());
        }
        T found = lookup.get();
        writer.remember(cache, key, found);
        return found;
    }

    /** The cache of the request of this thread, or null outside a request and after its end. */
    static @Nullable RequestCache liveCache() {
        RequestCache cache = RequestCacheScope.current();
        if (cache == null || cache.isDead()) {
            return null;
        }
        return cache;
    }

    @SuppressWarnings("unchecked")
    private static <T> @Nullable T valueOf(Object cached) {
        if (cached == RequestCache.MISS) {
            return null;
        }
        return (T) cached;
    }

    /** Evicts a key from the cache of the request. Does nothing without a key. */
    static void evict(@Nullable Object key) {
        RequestCache cache = liveCache();
        if (cache != null && key != null) {
            cache.evict(key);
        }
    }

    /** Reads the answer of a lookup from the cache, as {@link RequestCache#found(Object)} does. */
    @FunctionalInterface
    interface AnswerReader {
        Optional<Object> cachedAnswer(RequestCache cache, Object key);
    }

    /** Caches the answer of a lookup, a miss if it found nothing, as {@link RequestCache#put(Object, Object)} does. */
    @FunctionalInterface
    interface AnswerWriter {
        void remember(RequestCache cache, Object key, @Nullable Object found);
    }
}
