/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.bus;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Predicates;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Configuration
@Slf4j
public class BusEventCollector {

    private BlockingQueue<RemoteGeoServerEvent> events = new LinkedBlockingQueue<>();

    private @Value("${spring.cloud.bus.id}") String busId;
    private @Autowired RemoteGeoServerEventBridge bridge;

    private @NonNull Class<? extends InfoEvent> eventType = InfoEvent.class;

    private volatile boolean capturing = false;

    @EventListener(RemoteGeoServerEvent.class)
    public void onApplicationEvent(RemoteGeoServerEvent busEvent) {
        if (!capturing) {
            log.debug("{}: capturing is off, ignoring {}", busId, busEvent);
            return;
        }
        GeoServerEvent payloadEvent = busEvent.getEvent();
        if (!eventType.isInstance(payloadEvent)) {
            log.debug(
                    "{}: ignoring non {} event {}", busId, eventType.getSimpleName(), payloadEvent);
            return;
        }
        log.info("{}: captured event {}", busId, busEvent);
        events.add(busEvent);
    }

    public void capture(@NonNull Class<? extends InfoEvent> type) {
        this.eventType = type;
    }

    public <T extends InfoEvent> RemoteGeoServerEvent expectOne(Class<T> payloadType) {
        return expectOne(payloadType, Predicates.alwaysTrue());
    }

    public <T extends InfoEvent> RemoteGeoServerEvent expectOne(
            Class<T> payloadType, ConfigInfoType infoType) {
        return expectOne(payloadType, c -> infoType.equals(c.getObjectType()));
    }

    public <T extends InfoEvent> RemoteGeoServerEvent expectOne(
            Class<T> payloadType, Predicate<T> filter) {
        final long t = System.nanoTime();
        final long max = t + TimeUnit.SECONDS.toNanos(5);
        List<RemoteGeoServerEvent> list = allOf(payloadType);
        while (list.stream()
                        .map(RemoteGeoServerEvent::getEvent)
                        .filter(payloadType::isInstance)
                        .map(payloadType::cast)
                        .noneMatch(filter)
                && System.nanoTime() < max) {
            try {
                Thread.sleep(10);
                list = allOf(payloadType);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        List<RemoteGeoServerEvent> matches =
                list.stream()
                        .filter(re -> payloadType.isInstance(re.getEvent()))
                        .filter(e -> filter.test((T) e.getEvent()))
                        .toList();

        String message =
                "expected 1, got "
                        + matches.size()
                        + " events of type "
                        + payloadType.getSimpleName()
                        + ": "
                        + matches;
        assertEquals(message, 1, matches.size());
        return matches.get(0);
    }

    public <T extends InfoEvent> List<RemoteGeoServerEvent> allOf(Class<T> payloadType) {
        return capturedEvents(payloadType).toList();
    }

    public <T extends InfoEvent> Optional<RemoteGeoServerEvent> first(Class<T> payloadType) {
        return capturedEvents(payloadType).findFirst();
    }

    private <T extends InfoEvent> Stream<RemoteGeoServerEvent> capturedEvents(
            Class<T> payloadType) {
        return capturedEvents().filter(remote -> payloadType.isInstance(remote.getEvent()));
    }

    private Stream<RemoteGeoServerEvent> capturedEvents() {
        return events.stream();
    }

    public void clear() {
        events = new LinkedBlockingQueue<>();
    }

    public void stop() {
        log.debug("bus id {}: stopped", busId);
        capturing = false;
        bridge.enabled(false);
    }

    public void start() {
        log.debug("bus id {}: ready to capture {} events", busId, eventType.getSimpleName());
        capturing = true;
        bridge.enabled(true);
    }
}
