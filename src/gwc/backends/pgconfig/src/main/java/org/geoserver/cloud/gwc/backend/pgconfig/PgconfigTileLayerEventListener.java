/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.backend.pgconfig;

import lombok.NonNull;
import org.geoserver.catalog.plugin.requestcache.RemoteChangeCounter;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Drops the cached GWC settings of a tile layer when a {@link TileLayerEvent} reports a change.
 *
 * <p>A change made through another service instance arrives as a remote event, and this instance cannot see it
 * otherwise: its entity cache keeps the old settings. After the eviction, a remote event also moves the
 * {@link RemoteChangeCounter}. The requests running at that moment then read the settings again.
 *
 * <p>The listener runs before the {@link BuiltTileLayerCache} clears itself on the same event. A tile layer built after
 * the clear then reads the new settings.
 */
public class PgconfigTileLayerEventListener {

    private final @Nullable EntityCache cache;
    private final RemoteChangeCounter remoteChanges;

    /** @param cache the entity cache holding the settings, or null if the catalog has none */
    public PgconfigTileLayerEventListener(@Nullable EntityCache cache, @NonNull RemoteChangeCounter remoteChanges) {
        this.cache = cache;
        this.remoteChanges = remoteChanges;
    }

    @EventListener(TileLayerEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void onTileLayerEvent(TileLayerEvent event) {
        evictSettings(event);
        if (event.isRemote()) {
            remoteChanges.increment();
        }
    }

    private void evictSettings(TileLayerEvent event) {
        if (cache != null) {
            cache.evict(CacheKey.tileLayer(event.getPublishedId()));
        }
    }
}
