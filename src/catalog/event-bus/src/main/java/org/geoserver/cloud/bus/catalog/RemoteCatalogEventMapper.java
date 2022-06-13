/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.catalog;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.cloud.event.info.InfoEvent;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.event.Destination;

import java.util.Optional;

/** */
@RequiredArgsConstructor
public class RemoteCatalogEventMapper {

    /** Constant indicating a remote event is destined to all services */
    private static final String DESTINATION_ALL_SERVICES = "**";

    private final @NonNull InfoEventResolver remoteEventsPropertyResolver;
    private final @NonNull ServiceMatcher serviceMatcher;
    private final @NonNull Destination.Factory destinationFactory;

    private Destination destinationService() {
        return destinationFactory.getDestination(DESTINATION_ALL_SERVICES);
    }

    private @NonNull String originService() {
        return serviceMatcher.getBusId();
    }

    public RemoteInfoEvent toRemote(InfoEvent<?, ?, ?> anyLocalCatalogOrConfigEvent) {
        String origin = originService();
        Destination destination = destinationService();
        Object source = new Object(); // anyLocalCatalogOrConfigEvent.getSource();
        return new RemoteInfoEvent(source, anyLocalCatalogOrConfigEvent, origin, destination);
    }

    public Optional<InfoEvent<?, ?, ?>> toLocal(@NonNull RemoteInfoEvent incoming) {
        final boolean fromSelf = serviceMatcher.isFromSelf(incoming);
        if (!fromSelf) {
            InfoEvent<?, ?, ?> event = incoming.getEvent();
            event.setRemote(true);
            event = remoteEventsPropertyResolver.resolve(event);
            return Optional.of(event);
        }
        return Optional.empty();
    }
}
