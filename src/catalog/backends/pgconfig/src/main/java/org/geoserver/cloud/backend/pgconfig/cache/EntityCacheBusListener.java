/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.event.bus.RemoteEventsResumed;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Empties caches of the pgconfig backend, such as the entity caches, when the events of the other service instances
 * reach this one again: at startup, and after a lost connection to the event bus.
 *
 * <p>The events sent while the consumer of the bus was away are lost, and the entries made stale by them would stay
 * until another event or a clear removes them. The caches are emptied before the other listeners of
 * {@link RemoteEventsResumed} run: a listener rebuilding its state from the catalog must not read the stale entries.
 */
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.cache")
public class EntityCacheBusListener {

    private final List<ClearableCache> caches;

    public EntityCacheBusListener(@NonNull List<? extends ClearableCache> caches) {
        this.caches = List.copyOf(caches);
    }

    @EventListener(RemoteEventsResumed.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void onRemoteEventsResumed(RemoteEventsResumed event) {
        for (ClearableCache cache : caches) {
            cache.clear();
        }
        List<String> names = caches.stream().map(ClearableCache::name).toList();
        log.info("Cleared the caches {}: a consumer subscribed to the queue {}", names, event.queue());
    }
}
