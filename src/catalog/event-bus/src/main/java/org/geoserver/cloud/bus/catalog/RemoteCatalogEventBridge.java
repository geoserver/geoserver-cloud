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
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.info.InfoPreModifyEvent;
import org.springframework.context.event.EventListener;

import java.util.function.Consumer;

/**
 * Listens to local catalog and configuration change {@link InfoEvent}s produced by this service
 * instance and broadcasts them to the cluster as {@link RemoteInfoEvent}
 */
@Slf4j(topic = "org.geoserver.cloud.bus.outgoing")
@RequiredArgsConstructor
public class RemoteCatalogEventBridge {

    private final @NonNull Consumer<RemoteInfoEvent> remoteEventPublisher;
    private final @NonNull Consumer<InfoEvent<?, ?, ?>> localRemoteEventPublisher;

    private final @NonNull RemoteCatalogEventMapper mapper;
    private boolean enabled = true;

    public @VisibleForTesting void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    @EventListener(InfoEvent.class)
    public void handleLocalEvent(InfoEvent<? extends InfoEvent<?, ?, ?>, ?, ?> event)
            throws CatalogException {
        if (enabled) {
            event.local()
                    .filter(e -> !(e instanceof InfoPreModifyEvent))
                    .map(mapper::toRemote)
                    .ifPresent(this::publishRemoteEvent);
        }
    }

    @EventListener(RemoteInfoEvent.class)
    public void handleRemoteEvent(RemoteInfoEvent incoming) throws CatalogException {
        mapper.toLocal(incoming).ifPresent(this::publishLocalEvent);
    }

    private void publishRemoteEvent(RemoteInfoEvent remoteEvent) {
        log.debug("broadcasting {}", remoteEvent);
        try {
            remoteEventPublisher.accept(remoteEvent);
        } catch (RuntimeException e) {
            log.error("Error publishing remote event {}", remoteEvent, e);
            throw e;
        }
    }

    private void publishLocalEvent(InfoEvent<?, ?, ?> localRemoteEvent) {
        log.debug("publishing remote event {}", localRemoteEvent);
        try {
            localRemoteEventPublisher.accept(localRemoteEvent);
            ;
        } catch (RuntimeException e) {
            log.error("Error accepting remote event {}", localRemoteEvent, e);
            throw e;
        }
    }
}
