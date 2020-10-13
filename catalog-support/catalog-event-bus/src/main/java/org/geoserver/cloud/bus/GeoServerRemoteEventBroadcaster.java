/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus;

import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.catalog.plugin.PropertyDiff.Change;
import org.geoserver.cloud.bus.event.RemoteAddEvent;
import org.geoserver.cloud.bus.event.RemoteInfoEvent;
import org.geoserver.cloud.bus.event.RemoteModifyEvent;
import org.geoserver.cloud.bus.event.RemoteRemoveEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogAddEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogInfoModifyEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogModifyEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogRemoveEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteDefaultDataStoreEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteDefaultNamespaceEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteDefaultWorkspaceEvent;
import org.geoserver.cloud.bus.event.config.RemoteConfigAddEvent;
import org.geoserver.cloud.bus.event.config.RemoteConfigModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteConfigRemoveEvent;
import org.geoserver.cloud.bus.event.config.RemoteGeoSeverInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteLoggingInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteServiceInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteSettingsInfoModifyEvent;
import org.geoserver.cloud.event.LocalInfoEvent;
import org.geoserver.cloud.event.catalog.LocalCatalogAddEvent;
import org.geoserver.cloud.event.catalog.LocalCatalogPostModifyEvent;
import org.geoserver.cloud.event.catalog.LocalCatalogRemoveEvent;
import org.geoserver.cloud.event.config.LocalConfigAddEvent;
import org.geoserver.cloud.event.config.LocalConfigPostModifyEvent;
import org.geoserver.cloud.event.config.LocalConfigRemoveEvent;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
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
    private @Autowired BusProperties springBusProperties;

    private @Autowired GeoServerBusProperties geoserverBusProperties;

    public String toString() {
        return String.format("%s(%s)", getClass().getSimpleName(), springBusProperties.getId());
    }

    private String destinationService() {
        return DESTINATION_ALL_SERVICES;
    }

    private @NonNull String originService() {
        return springBusProperties.getId();
    }

    private void publishRemoteEvent(
            LocalInfoEvent<?, ?> localEvent, RemoteInfoEvent<?, ?> remoteEvent) {
        if (!geoserverBusProperties.isSendEvents()) {
            return;
        }
        if (!geoserverBusProperties.isSendObject()) {
            if (remoteEvent instanceof RemoteAddEvent) {
                ((RemoteAddEvent<?, ?>) remoteEvent).setObject(null);
            }
            if (remoteEvent instanceof RemoteRemoveEvent) {
                ((RemoteRemoveEvent<?, ?>) remoteEvent).setObject(null);
            }
        }
        if (remoteEvent instanceof RemoteModifyEvent) {
            RemoteModifyEvent<?, ?> modifyEvent = (RemoteModifyEvent<?, ?>) remoteEvent;
            if (!geoserverBusProperties.isSendDiff()) {
                modifyEvent.setPatch(null);
            } else {
                Optional<Patch> patch = modifyEvent.patch();
                if (patch.isPresent() && patch.get().isEmpty()) {
                    log.debug(
                            "Not publishing remote event for no-op local change event {}",
                            localEvent);
                    return;
                }
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
        PropertyDiff diff = event.getDiff();
        Patch patch = diff.clean().toPatch();
        if (patch.isEmpty()) {
            log.debug(
                    "Ignoring no-op event on {}: {}",
                    RemoteCatalogEvent.resolveId(event.getObject()),
                    diff);
            return;
        }

        RemoteCatalogModifyEvent remoteEvent;
        if (event.getObject() instanceof Catalog) {
            remoteEvent = createCatalogChangedEvent(event, diff, patch);
        } else {
            remoteEvent =
                    beanFactory.getBean(
                            RemoteCatalogInfoModifyEvent.class,
                            event.getSource(),
                            event.getObject(),
                            patch,
                            originService(),
                            destinationService());
        }
        publishRemoteEvent(event, remoteEvent);
    }

    private RemoteCatalogModifyEvent createCatalogChangedEvent(
            @NonNull LocalCatalogPostModifyEvent event,
            @NonNull PropertyDiff diff,
            @NonNull Patch patch) {
        if (diff.get("defaultWorkspace").isPresent())
            return beanFactory.getBean(
                    RemoteDefaultWorkspaceEvent.class,
                    event.getSource(),
                    patch,
                    originService(),
                    destinationService());

        if (diff.get("defaultNamespace").isPresent())
            return beanFactory.getBean(
                    RemoteDefaultNamespaceEvent.class,
                    event.getSource(),
                    patch,
                    originService(),
                    destinationService());

        Optional<Change> defaultDataStoreProp = diff.get("defaultDataStore");
        if (defaultDataStoreProp.isPresent()) {
            Change change = defaultDataStoreProp.get();
            DataStoreInfo oldValue = (DataStoreInfo) change.getOldValue();
            DataStoreInfo newValue = (DataStoreInfo) change.getNewValue();
            // both can't be null because a no-op change shall have been ignored before reaching
            // here
            WorkspaceInfo workspace =
                    oldValue == null ? newValue.getWorkspace() : oldValue.getWorkspace();
            return beanFactory.getBean(
                    RemoteDefaultDataStoreEvent.class,
                    event.getSource(),
                    workspace,
                    patch,
                    originService(),
                    destinationService());
        }

        throw new IllegalArgumentException("Unknown Catalog event type: " + diff);
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

        Patch patch = event.getDiff().clean().toPatch();

        Class<? extends RemoteConfigModifyEvent<?>> eventType;
        if (event.getObject() instanceof GeoServerInfo)
            eventType = RemoteGeoSeverInfoModifyEvent.class;
        else if (event.getObject() instanceof ServiceInfo)
            eventType = RemoteServiceInfoModifyEvent.class;
        else if (event.getObject() instanceof SettingsInfo)
            eventType = RemoteSettingsInfoModifyEvent.class;
        else if (event.getObject() instanceof LoggingInfo)
            eventType = RemoteLoggingInfoModifyEvent.class;
        else throw new IllegalArgumentException("Unknown config info type: " + event.getObject());

        RemoteConfigModifyEvent<?> remoteEvent =
                this.beanFactory.getBean(
                        eventType,
                        event.getSource(),
                        event.getObject(),
                        patch,
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
