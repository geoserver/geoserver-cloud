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
import org.geoserver.cloud.autoconfigure.bus.RemoteInfoEventInboundResolver;
import org.mockito.Mockito;
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

    private RemoteInfoEventInboundResolver inboundEventResolver;

    public BusChannelEventCollector(
            BlockingQueue<Message<?>> outChannel,
            AbstractMessageConverter busJsonConverter,
            RemoteInfoEventInboundResolver inboundEventResolver) {
        this.sentMessages = outChannel;
        this.busJsonConverter = busJsonConverter;
        this.inboundEventResolver = inboundEventResolver;
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

    /**
     * Parses the message and fakes its {@link RemoteApplicationEvent#getOriginService() origin
     * service} to fake it coming from the wire, letting {@link
     * RemoteInfoEventInboundResolver#resolve(RemoteInfoEvent)} initialize the payload
     */
    private RemoteApplicationEvent parseEvent(Message<?> message) {
        Object fromMessage = busJsonConverter.fromMessage(message, RemoteApplicationEvent.class);
        if (fromMessage == null) {
            throw new IllegalStateException("Unable to parse message " + message);
        }
        RemoteApplicationEvent event = (RemoteApplicationEvent) fromMessage;
        event = Mockito.spy(event);
        Mockito.doReturn("mock-remote-service").when(event).getOriginService();
        if (event instanceof RemoteInfoEvent) {
            this.inboundEventResolver.resolve((RemoteInfoEvent<?, ?>) event);
        }
        return event;
    }
}
