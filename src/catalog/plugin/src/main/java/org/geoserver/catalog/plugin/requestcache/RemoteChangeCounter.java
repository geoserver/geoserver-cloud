/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Counts the changes made through other service instances, as their events arrive. A {@link RequestCache} records the
 * count when it opens. A lookup finding the count moved empties the cache once, and records the new count: the request
 * reads the changed objects again.
 *
 * <p>The caches opened by {@link RequestCacheScope} follow the counter of the JVM, see {@link #shared()}. The backend
 * moves it once per event, after its own caches dropped the changed objects.
 */
public final class RemoteChangeCounter {

    private static final RemoteChangeCounter SHARED = new RemoteChangeCounter();

    private final AtomicLong count = new AtomicLong();

    /** The counter of the JVM, followed by the caches opened by {@link RequestCacheScope}. */
    public static RemoteChangeCounter shared() {
        return SHARED;
    }

    /** Counts one change made through another service instance. */
    public void increment() {
        count.incrementAndGet();
    }

    /** The number of changes counted. */
    public long count() {
        return count.get();
    }
}
