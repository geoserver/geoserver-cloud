/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.bus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.*;

import static java.util.function.Predicate.not;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.lifecycle.LifecycleEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Configuration
@Slf4j
public class BusEventCollector {

    private BlockingQueue<RemoteGeoServerEvent> events = new LinkedBlockingQueue<>();

    private @Value("${spring.cloud.bus.id}") String busId;
    private @Autowired RemoteGeoServerEventBridge bridge;

    private @NonNull Class<? extends GeoServerEvent> eventType = GeoServerEvent.class;

    private volatile boolean capturing = false;

    @EventListener(RemoteGeoServerEvent.class)
    public void onApplicationEvent(RemoteGeoServerEvent busEvent) {
        if (capturing) {
            GeoServerEvent payloadEvent = busEvent.getEvent();
            if (eventType.isInstance(payloadEvent)) {
                log.info("{}: captured event {}", busId, busEvent);
                events.add(busEvent);
            } else {
                log.debug(
                        "{}: ignoring non {} event {}",
                        busId,
                        eventType.getSimpleName(),
                        payloadEvent);
            }
        } else {
            log.debug("{}: capturing is off, ignoring {}", busId, busEvent);
            return;
        }
    }

    public void capture(@NonNull Class<? extends InfoEvent> type) {
        this.eventType = type;
    }

    public void captureLifecycle(@NonNull Class<? extends LifecycleEvent> type) {
        this.eventType = type;
    }

    public <T extends InfoEvent> RemoteGeoServerEvent expectOne(Class<T> payloadType) {

        return expectOne(payloadType, x -> true);
    }

    public <T extends InfoEvent> RemoteGeoServerEvent expectOne(
            Class<T> payloadType, ConfigInfoType infoType) {
        return expectOne(payloadType, c -> infoType.equals(c.getObjectType()));
    }

    public <T extends InfoEvent> RemoteGeoServerEvent expectOne(
            Class<T> payloadType, Predicate<T> filter) {

        List<RemoteGeoServerEvent> matches =
                await().atMost(Duration.ofSeconds(10)) //
                        .until(() -> allOf(payloadType, filter), not(List::isEmpty));

        Supplier<String> message =
                () ->
                        "expected 1, got %d events of type %s : %s"
                                .formatted(matches.size(), payloadType.getSimpleName(), matches);

        assertThat(matches.size()).as(message).isOne();
        return matches.get(0);
    }

    public <T extends LifecycleEvent> RemoteGeoServerEvent expectOneLifecycleEvent(
            Class<T> payloadType) {

        List<RemoteGeoServerEvent> matches =
                await().atMost(Duration.ofSeconds(10)) //
                        .until(
                                () -> allOfLifecycle(payloadType, filter -> true),
                                not(List::isEmpty));

        assertThat(matches.size()).isOne();

        //noinspection OptionalGetWithoutIsPresent
        return matches.stream().findFirst().get();
    }

    public <T extends GeoServerEvent> List<RemoteGeoServerEvent> allOf(
            Class<T> payloadEventType, Predicate<T> eventFilter) {

        return capturedEvents(payloadEventType)
                .filter(
                        remoteEvent ->
                                eventFilter.test(payloadEventType.cast(remoteEvent.getEvent())))
                .toList();
    }

    public <T extends LifecycleEvent> List<RemoteGeoServerEvent> allOfLifecycle(
            Class<T> payloadEventType, Predicate<T> eventFilter) {

        return capturedLifecycleEvents(payloadEventType)
                .filter(
                        remoteEvent ->
                                eventFilter.test(payloadEventType.cast(remoteEvent.getEvent())))
                .toList();
    }

    public <T extends GeoServerEvent> List<RemoteGeoServerEvent> allOf(Class<T> payloadType) {
        return capturedEvents(payloadType).toList();
    }

    public <T extends GeoServerEvent> Optional<RemoteGeoServerEvent> first(Class<T> payloadType) {
        return capturedEvents(payloadType).findFirst();
    }

    private <T extends GeoServerEvent> Stream<RemoteGeoServerEvent> capturedEvents(
            Class<T> payloadType) {
        return capturedEvents().filter(remote -> payloadType.isInstance(remote.getEvent()));
    }

    private <T extends LifecycleEvent> Stream<RemoteGeoServerEvent> capturedLifecycleEvents(
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
        bridge.disable();
    }

    public void start() {
        log.debug("bus id {}: ready to capture {} events", busId, eventType.getSimpleName());
        capturing = true;
        bridge.enable();
    }
}
