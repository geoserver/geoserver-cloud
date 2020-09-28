/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus;

import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.cloud.bus.event.RemoteInfoEvent;
import org.geoserver.cloud.bus.event.RemoteModifyEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogAddEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogModifyEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogRemoveEvent;
import org.geoserver.cloud.bus.event.config.RemoteConfigAddEvent;
import org.geoserver.cloud.bus.event.config.RemoteConfigModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteConfigRemoveEvent;
import org.geoserver.cloud.event.LocalInfoEvent;
import org.geoserver.cloud.event.catalog.LocalCatalogAddEvent;
import org.geoserver.cloud.event.catalog.LocalCatalogPostModifyEvent;
import org.geoserver.cloud.event.catalog.LocalCatalogRemoveEvent;
import org.geoserver.cloud.event.config.LocalConfigAddEvent;
import org.geoserver.cloud.event.config.LocalConfigPostModifyEvent;
import org.geoserver.cloud.event.config.LocalConfigRemoveEvent;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

/**
 * Listens to local catalog change {@link ApplicationEvent}s produced by this service instance and
 * broadcasts them to the cluster as {@link RemoteCatalogEvent catalog remote events}
 *
 * @see LocalCatalogAddEvent
 * @see RemoteCatalogAddEvent
 * @see LocalCatalogRemoveEvent
 * @see RemoteCatalogRemoveEvent
 * @see LocalCatalogPostModifyEvent
 * @see RemoteCatalogModifyEvent
 * @see LocalConfigAddEvent
 * @see RemoteConfigAddEvent
 * @see LocalConfigPostModifyEvent
 * @see RemoteConfigModifyEvent
 * @see LocalConfigRemoveEvent
 * @see RemoteConfigRemoveEvent
 */
@Slf4j(topic = "org.geoserver.cloud.bus.outgoing")
public class GeoServerRemoteEventBroadcaster {

    /** Constant indicating a remote event is destined to all services */
    private static final String DESTINATION_ALL_SERVICES = null;

    /** The event publisher, usually the {@link ApplicationContext} itself */
    private @Autowired ApplicationEventPublisher eventPublisher;

    private @Autowired BeanFactory beanFactory;

    /**
     * Properties shared with {@link BusAutoConfiguration} in order to get the {@link
     * BusProperties#getId() service-id} used to identify the origin of {@link
     * RemoteApplicationEvent remote-events}. Such events ought to be constructed with that service
     * id to be properly broadcasted to other nodes but ignored on the sending node.
     */
    private @Autowired BusProperties busProperties;

    public String toString() {
        return String.format("%s(%s)", getClass().getSimpleName(), busProperties.getId());
    }

    private String destinationService() {
        return DESTINATION_ALL_SERVICES;
    }

    private @NonNull String originService() {
        return busProperties.getId();
    }

    private void publishRemoteEvent(
            LocalInfoEvent<?, ?> localEvent, RemoteInfoEvent<?, ?> remoteEvent) {
        if (remoteEvent instanceof RemoteModifyEvent) {
            Optional<PropertyDiff> diff = ((RemoteModifyEvent<?, ?>) remoteEvent).diff();
            if (diff.isPresent() && diff.get().isEmpty()) {
                log.debug(
                        "Not publishing remote event for no-op local change event {}", localEvent);
                return;
            }
        }
        log.debug("broadcasting {} upon {}", remoteEvent, localEvent.getClass().getSimpleName());
        try {
            eventPublisher.publishEvent(remoteEvent);
        } catch (RuntimeException e) {
            log.error("Error publishing remote event {}", remoteEvent, e);
            throw e;
        }
    }

    @EventListener(LocalCatalogAddEvent.class)
    public void onCatalogInfoAdded(LocalCatalogAddEvent event) throws CatalogException {
        RemoteCatalogAddEvent remoteEvent =
                beanFactory.getBean(
                        RemoteCatalogAddEvent.class,
                        event.getSource(),
                        event.getObject(),
                        originService(),
                        destinationService());
        publishRemoteEvent(event, remoteEvent);
    }

    @EventListener(LocalCatalogRemoveEvent.class)
    public void onCatalogInfoRemoved(LocalCatalogRemoveEvent event) throws CatalogException {
        RemoteCatalogRemoveEvent remoteEvent =
                beanFactory.getBean(
                        RemoteCatalogRemoveEvent.class,
                        event.getSource(),
                        event.getObject(),
                        originService(),
                        destinationService());
        publishRemoteEvent(event, remoteEvent);
    }

    @EventListener(LocalCatalogPostModifyEvent.class)
    public void onCatalogInfoModified(LocalCatalogPostModifyEvent event) throws CatalogException {
        RemoteCatalogModifyEvent remoteEvent =
                beanFactory.getBean(
                        RemoteCatalogModifyEvent.class,
                        event.getSource(),
                        event.getObject(),
                        event.getDiff().clean(),
                        originService(),
                        destinationService());
        publishRemoteEvent(event, remoteEvent);
    }

    @EventListener(LocalConfigAddEvent.class)
    public void onConfigInfoAdded(LocalConfigAddEvent event) throws CatalogException {
        RemoteConfigAddEvent remoteEvent =
                beanFactory.getBean(
                        RemoteConfigAddEvent.class,
                        event.getSource(),
                        event.getObject(),
                        originService(),
                        destinationService());
        publishRemoteEvent(event, remoteEvent);
    }

    @EventListener(LocalConfigPostModifyEvent.class)
    public void onConfigInfoModified(LocalConfigPostModifyEvent event) throws CatalogException {
        RemoteConfigModifyEvent remoteEvent =
                this.beanFactory.getBean(
                        RemoteConfigModifyEvent.class,
                        event.getSource(),
                        event.getObject(),
                        event.getDiff().clean(),
                        originService(),
                        destinationService());
        publishRemoteEvent(event, remoteEvent);
    }

    @EventListener(LocalConfigRemoveEvent.class)
    public void onConfigInfoRemoved(LocalConfigRemoveEvent event) throws CatalogException {
        RemoteConfigRemoveEvent remoteEvent =
                beanFactory.getBean(
                        RemoteConfigRemoveEvent.class,
                        event.getSource(),
                        event.getObject(),
                        originService(),
                        destinationService());
        publishRemoteEvent(event, remoteEvent);
    }
}
