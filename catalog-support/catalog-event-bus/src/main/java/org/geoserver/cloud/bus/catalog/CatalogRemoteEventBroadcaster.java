/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.catalog;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.cloud.bus.catalog.CatalogRemoteEvent.CatalogRemoteEventFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Listens to {@link CatalogEvent catalog events} produced by this service instance and broadcasts
 * them to the cluster as {@link CatalogRemoteEvent catalog remote events}
 *
 * <p>GeoServer {@link Catalog} does not publish spring {@link ApplicationEvent}s, but {@link
 * CatalogEvent}s to the {@link CatalogListener}s in the spring context. This {@code
 * CatalogListener} takes those events when changes to the catalog are performed on this instance
 * and publishes them to the cluster event bus.
 */
@Slf4j(topic = "org.geoserver.cloud.catalog.bus.outgoing")
public class CatalogRemoteEventBroadcaster implements CatalogListener {

    /** The event publisher, usually the {@link ApplicationContext} itself */
    private @Autowired ApplicationEventPublisher eventPublisher;

    /**
     * Properties shared with {@link BusAutoConfiguration} in order to get the {@link
     * BusProperties#getId() service-id} used to identify the origin of {@link
     * RemoteApplicationEvent remote-events}. Such events ought to be constructed with that service
     * id to be properly broadcasted to other nodes but ignored on the sending node.
     */
    private @Autowired BusProperties busProperties;

    public @Override String toString() {
        return String.format("%s(%s)", getClass().getSimpleName(), busProperties.getId());
    }

    public @Override void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        publishRemoteEvent(event, CatalogRemoteAddEvent::new);
    }

    public @Override void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        publishRemoteEvent(event, CatalogRemoteRemoveEvent::new);
    }

    public @Override void handlePostModifyEvent(CatalogPostModifyEvent event)
            throws CatalogException {
        publishRemoteEvent(event, CatalogRemoteModifyEvent::new);
    }

    private void publishRemoteEvent(
            CatalogEvent localEvent, CatalogRemoteEventFactory remoteEventFactory) {
        final CatalogInfo catalogInfo = localEvent.getSource();
        if (catalogInfo instanceof Catalog) {
            log.trace("ignoring CatalogImpl change event");
        } else {
            String originService = busProperties.getId();
            String destinationService = null; // all services
            String catalogInfoId = catalogInfo.getId();
            ClassMappings catalogInfoEnumType = interfaceOf(catalogInfo);

            CatalogRemoteEvent remoteEvent =
                    remoteEventFactory.create(
                            this,
                            originService,
                            destinationService,
                            catalogInfoId,
                            catalogInfoEnumType);
            log.info("broadcasting {} upon {}", remoteEvent, localEvent.getClass().getSimpleName());
            eventPublisher.publishEvent(remoteEvent);
        }
    }

    private @NonNull ClassMappings interfaceOf(@NonNull CatalogInfo source) {
        source = ModificationProxy.unwrap(source);
        ClassMappings mappings = ClassMappings.fromImpl(source.getClass());
        return mappings;
    }

    /** no-op, we only need to broadcast {@link CatalogPostModifyEvent post-modification} events */
    public @Override void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        // no-op
    }

    /** no-op */
    public @Override void reloaded() {
        // no-op
    }
}
