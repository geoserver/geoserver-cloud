/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

/**
 * A cache of the pgconfig backend emptied when the cached rows may have changed without notice, see
 * {@link EntityCacheBusListener}.
 */
public interface ClearableCache {

    /** Returns the name of the cache in the metrics, such as {@code gs-catalog}. */
    String name();

    /** Empties the cache. */
    void clear();
}
