/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.bus;

import com.google.common.annotations.VisibleForTesting;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.info.InfoModified;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.event.EventListener;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Listens to local catalog and configuration change {@link InfoEvent}s produced by this service
 * instance and broadcasts them to the cluster as {@link RemoteGeoServerEvent}
 */
public class RemoteGeoServerEventBridge {

    private final Outgoing outgoing;
    private final Incoming incoming;

    private boolean enabled = true;

    public RemoteGeoServerEventBridge( //
            @NonNull Consumer<GeoServerEvent<?>> localRemoteEventPublisher, //
            @NonNull Consumer<RemoteApplicationEvent> remoteEventPublisher, //
            @NonNull RemoteGeoServerEventMapper mapper, //
            @NonNull Supplier<String> localBusId) {

        this.outgoing = new Outgoing(remoteEventPublisher, mapper, localBusId);
        this.incoming = new Incoming(localRemoteEventPublisher, mapper, localBusId);
    }

    public @VisibleForTesting void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    @EventListener(GeoServerEvent.class)
    public void handleLocalEvent(GeoServerEvent<?> event) {
        if (enabled) {
            outgoing.broadCastIfLocal(event);
        }
    }

    @EventListener(RemoteGeoServerEvent.class)
    public void handleRemoteEvent(RemoteGeoServerEvent busEvent) throws CatalogException {
        if (enabled) {
            incoming.handleRemoteEvent(busEvent);
        }
    }

    @RequiredArgsConstructor
    @Slf4j(topic = "org.geoserver.cloud.event.bus.outgoing")
    private static class Outgoing {
        private final @NonNull Consumer<RemoteApplicationEvent> remoteEventPublisher;
        private final @NonNull RemoteGeoServerEventMapper mapper;
        private @NonNull Supplier<String> localBusId;

        public void broadCastIfLocal(GeoServerEvent<?> event) throws CatalogException {

            if (event.isLocal()) {
                RemoteGeoServerEvent remote = mapper.toRemote(event);
                publishRemoteEvent(remote);
            } else {
                log.trace("{}: not re-publishing {}", localBusId.get(), event);
            }
        }

        private void publishRemoteEvent(RemoteGeoServerEvent remoteEvent) {
            logOutgoing(remoteEvent);
            try {
                remoteEventPublisher.accept(remoteEvent);
            } catch (RuntimeException e) {
                log.error("{}: error broadcasting {}", localBusId.get(), remoteEvent, e);
                throw e;
            }
        }

        protected void logOutgoing(RemoteGeoServerEvent remoteEvent) {
            @NonNull GeoServerEvent<?> event = remoteEvent.getEvent();
            String logMsg = "{}: broadcasting {}";
            if (event instanceof InfoModified) {
                Patch patch = ((InfoModified<?, ?>) event).getPatch();
                if (patch.isEmpty()) {
                    logMsg = "{}: broadcasting no-change event {}";
                }
            }
            final String busId = localBusId.get();
            log.debug(logMsg, busId, remoteEvent);
        }
    }

    @RequiredArgsConstructor
    @Slf4j(topic = "org.geoserver.cloud.event.bus.incoming")
    private static class Incoming {

        private final @NonNull Consumer<GeoServerEvent<?>> localRemoteEventPublisher;

        private final @NonNull RemoteGeoServerEventMapper mapper;
        private @NonNull Supplier<String> localBusId;

        public void handleRemoteEvent(RemoteGeoServerEvent incoming) throws CatalogException {
            mapper.ifRemote(incoming) //
                    .ifPresentOrElse( //
                            this::publishLocalEvent, //
                            () ->
                                    log.trace(
                                            "{}: not broadcasting local-remote event {}",
                                            localBusId.get(),
                                            incoming));
        }

        private void publishLocalEvent(RemoteGeoServerEvent incoming) {
            log.trace("Received remote event {}", incoming);
            GeoServerEvent<?> localRemoteEvent = mapper.toLocalRemote(incoming);
            log.debug("{}: publishing as local event {}", localBusId.get(), incoming);
            try {
                localRemoteEventPublisher.accept(localRemoteEvent);
            } catch (RuntimeException e) {
                log.error("{}: error accepting remote {}", localBusId.get(), localRemoteEvent, e);
                throw e;
            }
        }
    }
}
