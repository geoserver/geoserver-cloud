/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import lombok.NonNull;
import org.geoserver.catalog.plugin.requestcache.RemoteChangeCounter;
import org.geoserver.catalog.plugin.requestcache.RequestCache;
import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

/**
 * Tells the request caches in flight about the changes made through other service instances, through a
 * {@link RemoteChangeCounter}. Without it, a request would keep the objects looked up before the change. A store
 * changed meanwhile would then reach the resource pool with its old connection parameters, and stay there until its
 * next change.
 *
 * <p>Each catalog or configuration event changes the update sequence: the listener counts one change per remote
 * {@link UpdateSequenceEvent}. It runs after {@link EntityCacheEventListener}: a request cache emptied by the count
 * reads the objects again from an entity cache without the changed objects.
 *
 * @see RequestCache
 */
public class RequestCacheEventListener {

    static final int ORDER = EntityCacheEventListener.ORDER + 1;

    private final RemoteChangeCounter remoteChanges;

    public RequestCacheEventListener(@NonNull RemoteChangeCounter remoteChanges) {
        this.remoteChanges = remoteChanges;
    }

    @EventListener(UpdateSequenceEvent.class)
    @Order(ORDER)
    public void onUpdateSequence(UpdateSequenceEvent event) {
        if (event.isRemote()) {
            remoteChanges.increment();
        }
    }
}
