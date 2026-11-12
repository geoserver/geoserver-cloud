/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.ConsumeOkEvent;
import org.springframework.context.event.EventListener;

/**
 * Empties the entity caches when a consumer of RabbitMQ subscribes to its queue: the consumer of the event bus, at
 * startup and again after a lost connection.
 *
 * <p>The event bus gives each service instance a queue of its own, deleted when its consumer leaves. The events sent
 * until the consumer subscribes again are lost, and the entries made stale by them would stay until they expire. Spring
 * AMQP publishes {@link ConsumeOkEvent} once the broker confirms the subscription: the queue exists and receives the
 * events sent from then on.
 */
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.cache")
public class EntityCacheBusListener {

    private final List<EntityCache> caches;

    public EntityCacheBusListener(@NonNull List<EntityCache> caches) {
        this.caches = List.copyOf(caches);
    }

    @EventListener(ConsumeOkEvent.class)
    public void onConsumerSubscribed(ConsumeOkEvent event) {
        for (EntityCache cache : caches) {
            cache.clear();
        }
        List<String> names = caches.stream().map(EntityCache::name).toList();
        log.info("Cleared the caches {}: a consumer subscribed to the queue {}", names, event.getQueue());
    }
}
