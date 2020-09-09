/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.cloud.bus.SpringCloudBusClient;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.context.ApplicationEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.AbstractMessageConverter;

/**
 * Uses {@code spring-cloud-stream-test-support} (must be a test dependency) to collect the
 * out-bound messages with a {@link MessageCollector}, but from the {@link
 * SpringCloudBusClient#OUTPUT} channel.
 *
 * <p>See <a href=
 * "https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/_testing.html">spring
 * streams testing</a> docs for reference.
 */
public class BusChannelEventCollector {
    /**
     * Message converter registered by spring-cloud-bus, used to parse the json message sent to the
     * bus and captured by {@link #messageCollector}
     */
    private AbstractMessageConverter busJsonConverter;

    private BlockingQueue<org.springframework.messaging.Message<?>> sentMessages;

    private RemoteEventPayloadCodec remoteEventPayloadCodec;

    public BusChannelEventCollector(
            BlockingQueue<Message<?>> outChannel,
            AbstractMessageConverter busJsonConverter,
            RemoteEventPayloadCodec remoteEventPayloadCodec) {
        this.sentMessages = outChannel;
        this.busJsonConverter = busJsonConverter;
        this.remoteEventPayloadCodec = remoteEventPayloadCodec;
    }

    public <T extends ApplicationEvent> T expectOne(Class<T> type) {
        List<T> list = allOf(type);
        assertEquals("exactly only one " + type.getSimpleName() + ": " + list, 1, list.size());
        return list.get(0);
    }

    public <T extends ApplicationEvent> List<T> allOf(Class<T> type) {
        return capturedEvents()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    public <T extends ApplicationEvent> Optional<T> first(Class<T> type) {
        return capturedEvents().filter(type::isInstance).map(type::cast).findFirst();
    }

    private Stream<RemoteApplicationEvent> capturedEvents() {
        return sentMessages.stream().map(this::parseEvent);
    }

    public void clear() {
        sentMessages.clear();
    }

    private RemoteApplicationEvent parseEvent(Message<?> message) {
        Object fromMessage = busJsonConverter.fromMessage(message, RemoteApplicationEvent.class);
        if (fromMessage instanceof RemoteInfoEvent) {
            remoteEventPayloadCodec.initIncomingMessage((RemoteInfoEvent<?, ?>) fromMessage);
        }
        return (RemoteApplicationEvent) fromMessage;
    }
}
