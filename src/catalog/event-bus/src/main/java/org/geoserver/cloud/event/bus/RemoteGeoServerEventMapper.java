/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.bus;

import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.info.InfoEvent;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.event.Destination;

/**
 * Aids {@link RemoteGeoServerEventBridge} in mapping {@link RemoteGeoServerEvent} to local {@link
 * GeoServerEvent} and vice-versa.
 *
 * @see InfoEventResolver
 * @see RemoteGeoServerEventBridge
 */
@RequiredArgsConstructor
class RemoteGeoServerEventMapper {

    /** Constant indicating a remote event is destined to all services */
    private static final String DESTINATION_ALL_SERVICES = "**";

    private final @NonNull InfoEventResolver remoteEventsPropertyResolver;
    private final @NonNull ServiceMatcher serviceMatcher;
    private final @NonNull Destination.Factory destinationFactory;

    private Destination destinationService() {
        return destinationFactory.getDestination(DESTINATION_ALL_SERVICES);
    }

    public @NonNull String localBusServiceId() {
        return serviceMatcher.getBusId();
    }

    public RemoteGeoServerEvent toRemote(GeoServerEvent anyLocalCatalogOrConfigEvent) {
        String origin = localBusServiceId();
        Destination destination = destinationService();
        RemoteGeoServerEvent remote = new RemoteGeoServerEvent(this, anyLocalCatalogOrConfigEvent, origin, destination);
        anyLocalCatalogOrConfigEvent.setOrigin(origin);
        anyLocalCatalogOrConfigEvent.setId(remote.getId());
        return remote;
    }

    public Optional<RemoteGeoServerEvent> ifRemote(@NonNull RemoteGeoServerEvent busEvent) {
        final boolean fromSelf = serviceMatcher.isFromSelf(busEvent);
        final boolean forSelf = serviceMatcher.isForSelf(busEvent);
        final boolean republishAsLocal = !fromSelf && forSelf;

        if (republishAsLocal) {
            GeoServerEvent event = busEvent.getEvent();
            event.setRemote(true);
            event.setOrigin(busEvent.getOriginService());
            return Optional.of(busEvent);
        }

        return Optional.empty();
    }

    public Optional<RemoteGeoServerEvent> mapIfLocal(GeoServerEvent event) {
        return Optional.of(event).filter(GeoServerEvent::isLocal).map(this::toRemote);
    }

    public GeoServerEvent toLocalRemote(@NonNull RemoteGeoServerEvent incoming) {
        GeoServerEvent event = incoming.getEvent();
        event.setRemote(true);
        event.setOrigin(incoming.getOriginService());
        if (event instanceof InfoEvent infoEvent) {
            event = remoteEventsPropertyResolver.resolve(infoEvent);
        }
        return event;
    }
}
