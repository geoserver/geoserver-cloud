/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.bus;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogException;
import org.geoserver.cloud.gwc.event.TileLayerAddedEvent;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.cloud.gwc.event.TileLayerModifiedEvent;
import org.geoserver.cloud.gwc.event.TileLayerRemovedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

/**
 * Listens to local {@link TileLayerEvent}s produced by this service instance and broadcasts them to
 * the cluster as {@link RemoteTileLayerEvent} on the event bus.
 */
@Slf4j(topic = "org.geoserver.cloud.bus.outgoing.gwc")
public class TileLayerRemoteEventBroadcaster {

    /** The event publisher, usually the {@link ApplicationContext} itself */
    private @Autowired ApplicationEventPublisher eventPublisher;

    private @Autowired ServiceMatcher busServiceMatcher;

    /**
     * Properties shared with {@link BusAutoConfiguration} in order to get the {@link
     * BusProperties#getId() service-id} used to identify the origin of {@link
     * RemoteApplicationEvent remote-events}. Such events ought to be constructed with that service
     * id to be properly broadcasted to other nodes but ignored on the sending node.
     */
    private @Autowired BusProperties springBusProperties;

    public @Override String toString() {
        return String.format("%s(%s)", getClass().getSimpleName(), springBusProperties.getId());
    }

    private @NonNull String originService() {
        return springBusProperties.getId();
    }

    @EventListener(TileLayerEvent.class)
    public void onTileLayerEvent(TileLayerEvent localEvent) throws CatalogException {
        if (isFromSelf(localEvent)) {
            RemoteTileLayerEvent remoteEvent = toRemote(localEvent);
            log.debug("publishing {} from {}", remoteEvent, localEvent);
            this.eventPublisher.publishEvent(remoteEvent);
        }
    }

    @EventListener(RemoteTileLayerEvent.class)
    public void onRemoteTileLayerEvent(RemoteTileLayerEvent remoteEvent) {
        if (!isFromSelf(remoteEvent)) {
            TileLayerEvent localEvent = toLocal(remoteEvent);
            log.debug("publishing {} from {}", localEvent, remoteEvent);
            this.eventPublisher.publishEvent(localEvent);
        }
    }

    private boolean isFromSelf(TileLayerEvent localEvent) {
        return localEvent.getSource() != this;
    }

    private boolean isFromSelf(RemoteTileLayerEvent remoteEvent) {
        return busServiceMatcher.isFromSelf(remoteEvent);
    }

    RemoteTileLayerEvent toRemote(@NonNull TileLayerEvent localEvent) {

        final String originService = originService();
        final Object source = localEvent.getSource();
        final String layerId = localEvent.getLayerId();
        switch (localEvent.getEventType()) {
            case CREATE:
                return new RemoteTileLayerAddedEvent(source, originService, layerId);
            case DELETE:
                return new RemoteTileLayerRemovedEvent(source, originService, layerId);
            case MODIFY:
                return new RemoteTileLayerModifiedEvent(source, originService, layerId);
            default:
                throw new IllegalArgumentException("type: " + localEvent.getEventType());
        }
    }

    TileLayerEvent toLocal(@NonNull RemoteTileLayerEvent remoteEvent) {
        final Object localSource = this;
        final String layerId = remoteEvent.getLayerId();
        switch (remoteEvent.getEventType()) {
            case CREATED:
                return new TileLayerAddedEvent(localSource, layerId);
            case DELETED:
                return new TileLayerRemovedEvent(localSource, layerId);
            case MODIFIED:
                return new TileLayerModifiedEvent(localSource, layerId);
            default:
                throw new IllegalArgumentException("type: " + remoteEvent.getEventType());
        }
    }
}
