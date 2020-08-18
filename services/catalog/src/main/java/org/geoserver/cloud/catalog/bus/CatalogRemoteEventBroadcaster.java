/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.catalog.bus;

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
import org.geoserver.cloud.catalog.bus.events.CatalogRemoteAddEvent;
import org.geoserver.cloud.catalog.bus.events.CatalogRemoteEvent;
import org.geoserver.cloud.catalog.bus.events.CatalogRemoteModifyEvent;
import org.geoserver.cloud.catalog.bus.events.CatalogRemoteRemoveEvent;
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
@Slf4j
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

    private void publishRemoteEvent(CatalogRemoteEvent remoteEvent) {
        log.info("broadcasting remote event {}", remoteEvent);
        eventPublisher.publishEvent(remoteEvent);
    }

    public @Override void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        String originService = busProperties.getId();
        String destinationService = null; // all services
        String catalogInfoId = event.getSource().getId();
        ClassMappings catalogInfoEnumType = interfaceOf(event.getSource());
        publishRemoteEvent(
                new CatalogRemoteAddEvent(
                        this,
                        originService,
                        destinationService,
                        catalogInfoId,
                        catalogInfoEnumType));
    }

    public @Override void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        String originService = busProperties.getId();
        String destinationService = null; // all services
        String catalogInfoId = event.getSource().getId();
        ClassMappings catalogInfoEnumType = interfaceOf(event.getSource());
        publishRemoteEvent(
                new CatalogRemoteRemoveEvent(
                        this,
                        originService,
                        destinationService,
                        catalogInfoId,
                        catalogInfoEnumType));
    }

    public @Override void handlePostModifyEvent(CatalogPostModifyEvent event)
            throws CatalogException {
        String originService = busProperties.getId();
        String destinationService = null; // all services
        String catalogInfoId = event.getSource().getId();
        ClassMappings catalogInfoEnumType = interfaceOf(event.getSource());
        publishRemoteEvent(
                new CatalogRemoteModifyEvent(
                        this,
                        originService,
                        destinationService,
                        catalogInfoId,
                        catalogInfoEnumType));
    }

    private ClassMappings interfaceOf(CatalogInfo source) {
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
