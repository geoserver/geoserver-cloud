/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import java.util.Map;
import org.geoserver.threadlocals.ThreadLocalTransfer;

/**
 * Hands the {@link RequestCache} of a request to the worker threads of the request, such as the WMS rendering threads,
 * the GWC seeding threads and the WPS processes. GeoServer runs the {@link ThreadLocalTransfer} beans of the
 * application context around the tasks of those pools.
 *
 * <p>The worker shares the cache with the request thread, and the request thread owns it: {@link #cleanup()} removes
 * the cache from the worker without killing it. A worker still running when the request ends holds a dead cache, and
 * reads through to the facade below.
 */
public class RequestCacheThreadLocalTransfer implements ThreadLocalTransfer {

    private static final String KEY = RequestCacheScope.class.getName() + "#CURRENT";

    /** Stores the cache of this thread, if its scope is open. */
    @Override
    public void collect(Map<String, Object> storage) {
        RequestCache cache = RequestCacheScope.current();
        if (cache != null && !cache.isDead()) {
            storage.put(KEY, cache);
        }
    }

    /**
     * Binds the collected cache to this thread. Without one, this thread gets none: a pooled thread may still hold the
     * cache of another task.
     */
    @Override
    public void apply(Map<String, Object> storage) {
        RequestCache cache = (RequestCache) storage.get(KEY);
        RequestCacheScope.apply(cache);
    }

    @Override
    public void cleanup() {
        RequestCacheScope.cleanup();
    }
}
