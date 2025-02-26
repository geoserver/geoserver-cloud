package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Least Recently Used (LRU) cache implementation using {@link LinkedHashMap}.
 * This cache evicts the least recently used entry when the cache size exceeds the
 * specified maximum capacity.
 *
 * <p>
 * This implementation leverages the {@link LinkedHashMap} with access order
 * enabled ({@code accessOrder = true}), which automatically moves entries to the
 * end of the list upon access (get or put), making the beginning of the list the
 * least recently used entry.
 * </p>
 *
 * <p>
 * The cache is initialized with a maximum capacity, and once this capacity is
 * exceeded, the least recently used entry is automatically removed.
 * </p>
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
@SuppressWarnings({"serial", "java:S2160"})
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    /** The maximum number of entries allowed in the cache */
    private final int maxCapacity;

    /**
     * Constructs an LRU cache with the specified maximum capacity.
     *
     * @param maxCapacity the maximum number of entries allowed in the cache
     */
    public LRUCache(int maxCapacity) {
        // Initialize LinkedHashMap with capacity, load factor 0.75, and access order enabled
        super(maxCapacity, 0.75f, true);
        this.maxCapacity = maxCapacity;
    }

    /**
     * Determines whether the eldest entry should be removed.
     * This method is called after a put operation and removes the eldest entry
     * if the cache size exceeds the maximum capacity.
     *
     * @param eldest the eldest entry (least recently used)
     * @return true if the eldest entry should be removed, false otherwise
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCapacity;
    }
}
