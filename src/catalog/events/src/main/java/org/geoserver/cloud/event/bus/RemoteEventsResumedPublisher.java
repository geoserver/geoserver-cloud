/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.event.bus;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.ConsumeOkEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

/**
 * Publishes {@link RemoteEventsResumed} when a consumer of RabbitMQ subscribes to its queue. Spring AMQP publishes
 * {@link ConsumeOkEvent} once the broker confirms the subscription: the queue exists and receives the events sent from
 * then on.
 *
 * <p>The event bus gives each service instance a queue of its own, deleted when its consumer leaves. The subscription
 * of another consumer of RabbitMQ is taken for a subscription of the bus: emptying a cache once more is harmless.
 */
@Slf4j(topic = "org.geoserver.cloud.event.bus")
public class RemoteEventsResumedPublisher {

    private final ApplicationEventPublisher publisher;

    public RemoteEventsResumedPublisher(@NonNull ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener(ConsumeOkEvent.class)
    public void onConsumerSubscribed(ConsumeOkEvent event) {
        log.info("A consumer subscribed to the queue {}, remote events resumed", event.getQueue());
        publisher.publishEvent(new RemoteEventsResumed(event.getQueue()));
    }
}
