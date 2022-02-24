/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.integration;

import static org.junit.Assert.assertEquals;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.bus.event.RemoteInfoEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@Slf4j
@SuppressWarnings("rawtypes")
public class BusEventCollector {

    private BlockingQueue<RemoteApplicationEvent> events = new LinkedBlockingQueue<>();

    private @Value("${spring.cloud.bus.id}") String busId;

    private Class<? extends RemoteInfoEvent> eventType = RemoteInfoEvent.class;

    private volatile boolean capturing = false;

    @EventListener(RemoteInfoEvent.class)
    public void onApplicationEvent(RemoteInfoEvent event) {
        log.debug("Received event on bus id {}: {}", busId, event);
        if (capturing && eventType.isInstance(event)) {
            log.info("Captured event on bus id {}: {}", busId, event);
            events.add(event);
        }
    }

    public void capture(@NonNull Class<? extends RemoteInfoEvent> type) {
        this.eventType = type;
    }

    public <T extends ApplicationEvent> T expectOne(Class<T> type) {
        final long t = System.nanoTime();
        final long max = t + TimeUnit.SECONDS.toNanos(5);
        List<T> list = allOf(type);
        while (list.isEmpty() && System.nanoTime() < max) {
            try {
                Thread.sleep(100);
                list = allOf(type);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        String message =
                "expected 1, got "
                        + list.size()
                        + " events of type "
                        + type.getSimpleName()
                        + ": "
                        + list;
        assertEquals(message, 1, list.size());
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

    private Stream<? extends RemoteApplicationEvent> capturedEvents() {
        return events.stream();
    }

    public void clear() {
        events = new LinkedBlockingQueue<>();
    }

    public void stop() {
        log.debug("bus id {}: stopped", busId);
        capturing = false;
    }

    public void start() {
        log.debug("bus id {}: ready to capture {} events", busId, eventType.getSimpleName());
        capturing = true;
    }
}
