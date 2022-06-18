/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.catalog;

import com.google.common.annotations.VisibleForTesting;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.info.InfoPostModifyEvent;
import org.geoserver.cloud.event.info.InfoPreModifyEvent;
import org.springframework.context.event.EventListener;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Listens to local catalog and configuration change {@link InfoEvent}s produced by this service
 * instance and broadcasts them to the cluster as {@link RemoteInfoEvent}
 */
public class RemoteCatalogEventBridge {

    private final Outgoing outgoing;
    private final Incoming incoming;

    private boolean enabled = true;

    @SuppressWarnings("rawtypes")
    public RemoteCatalogEventBridge( //
            @NonNull Consumer<InfoEvent> localRemoteEventPublisher, //
            @NonNull Consumer<RemoteInfoEvent> remoteEventPublisher, //
            @NonNull RemoteCatalogEventMapper mapper, //
            @NonNull Supplier<String> localBusId) {

        this.outgoing = new Outgoing(remoteEventPublisher, mapper, localBusId);
        this.incoming = new Incoming(localRemoteEventPublisher, mapper, localBusId);
    }

    public @VisibleForTesting void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    @EventListener(InfoEvent.class)
    public void handleLocalEvent(InfoEvent<? extends InfoEvent<?, ?>, ?> event) {
        if (enabled) {
            outgoing.broadCastIfLocal(event);
        }
    }

    @EventListener(RemoteInfoEvent.class)
    public void handleRemoteEvent(RemoteInfoEvent busEvent) throws CatalogException {
        if (enabled) {
            incoming.handleRemoteEvent(busEvent);
        }
    }

    @RequiredArgsConstructor
    @Slf4j(topic = "org.geoserver.cloud.bus.catalog.outgoing")
    private static class Outgoing {
        private final @NonNull Consumer<RemoteInfoEvent> remoteEventPublisher;
        private final @NonNull RemoteCatalogEventMapper mapper;
        private @NonNull Supplier<String> localBusId;

        public void broadCastIfLocal(InfoEvent<? extends InfoEvent<?, ?>, ?> event)
                throws CatalogException {

            event.local() //
                    .filter(e -> !(e instanceof InfoPreModifyEvent)) //
                    .map(mapper::toRemote) //
                    .ifPresentOrElse( //
                            this::publishRemoteEvent, //
                            () -> log.trace("{}: not re-publishing {}", localBusId.get(), event));
        }

        private void publishRemoteEvent(RemoteInfoEvent remoteEvent) {
            InfoEvent<?, ?> event = remoteEvent.getEvent();
            if (event instanceof InfoPostModifyEvent) {
                Patch patch = ((InfoPostModifyEvent<?, ?>) event).getPatch();
                if (patch.isEmpty()) {
                    log.info("Not broadcasting no-change event {}", remoteEvent);
                    return;
                }
            }
            log.debug("{}: broadcasting {}", localBusId.get(), remoteEvent);
            try {
                remoteEventPublisher.accept(remoteEvent);
            } catch (RuntimeException e) {
                log.error("{}: error broadcasting {}", localBusId.get(), remoteEvent, e);
                throw e;
            }
        }
    }

    @RequiredArgsConstructor
    @Slf4j(topic = "org.geoserver.cloud.bus.catalog.incoming")
    private static class Incoming {

        @SuppressWarnings("rawtypes")
        private final @NonNull Consumer<InfoEvent> localRemoteEventPublisher;

        private final @NonNull RemoteCatalogEventMapper mapper;
        private @NonNull Supplier<String> localBusId;

        public void handleRemoteEvent(RemoteInfoEvent incoming) throws CatalogException {
            mapper.ifRemote(incoming) //
                    .ifPresentOrElse( //
                            this::publishLocalEvent, //
                            () ->
                                    log.trace(
                                            "{}: not broadcasting local-remote event {}",
                                            localBusId.get(),
                                            incoming));
        }

        private void publishLocalEvent(RemoteInfoEvent incoming) {

            InfoEvent<?, ?> localRemoteEvent = mapper.toLocalRemote(incoming);
            log.debug("{}: publishing as local-remote {}", localBusId.get(), incoming);
            try {
                localRemoteEventPublisher.accept(localRemoteEvent);
            } catch (RuntimeException e) {
                log.error("{}: error accepting remote {}", localBusId.get(), localRemoteEvent, e);
                throw e;
            }
        }
    }
}
