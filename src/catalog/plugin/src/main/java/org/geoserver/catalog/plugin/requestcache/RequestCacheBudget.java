/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts the entries of the live request caches against one maximum. The cap of a {@link RequestCache} bounds the
 * memory of one request, and the budget bounds the memory of the requests in flight together. A cache takes a share of
 * the budget for each new entry, and gives it back for each entry it drops.
 *
 * <p>The caches opened by {@link RequestCacheScope} share the budget of the JVM, see {@link #shared()}. Once the budget
 * is used up, the caches reject new entries, as past their own cap, and keep answering from their entries.
 */
public final class RequestCacheBudget {

    /** The maximum number of entries of the live request caches of the JVM, unless configured otherwise. */
    public static final int DEFAULT_MAX_TOTAL_ENTRIES = 100_000;

    private static final RequestCacheBudget SHARED = new RequestCacheBudget(DEFAULT_MAX_TOTAL_ENTRIES);

    private final AtomicInteger used = new AtomicInteger();

    private volatile int maxTotalEntries;

    /** A budget of at most {@code maxTotalEntries} entries, for the caches given to it. */
    public RequestCacheBudget(int maxTotalEntries) {
        this.maxTotalEntries = maxTotalEntries;
    }

    /** The budget of the JVM, shared by the caches opened by {@link RequestCacheScope}. */
    public static RequestCacheBudget shared() {
        return SHARED;
    }

    /** The maximum number of entries of the caches of this budget together. */
    public int maxTotalEntries() {
        return maxTotalEntries;
    }

    /**
     * Sets the maximum number of entries. The entries held already stay: past a lower maximum, the caches take no new
     * entries until they drop enough of them.
     */
    public void setMaxTotalEntries(int maxTotalEntries) {
        this.maxTotalEntries = maxTotalEntries;
    }

    /** The number of entries held by the caches of this budget. */
    public int used() {
        return used.get();
    }

    /** Whether the caches of this budget hold its maximum number of entries. */
    public boolean isExhausted() {
        return used.get() >= maxTotalEntries;
    }

    /**
     * Takes the share of one entry. Returns false, and takes nothing, if the budget is used up. Unlike the cap of a
     * cache, the budget is checked and taken in one step. Many requests put at the same time, and a check without a
     * lock would let each of them pass the budget by one entry.
     */
    boolean tryTake() {
        int current = used.get();
        while (current < maxTotalEntries) {
            if (used.compareAndSet(current, current + 1)) {
                return true;
            }
            current = used.get();
        }
        return false;
    }

    /** Gives back the share of one entry dropped by a cache. */
    void release() {
        used.decrementAndGet();
    }
}
