/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.incoming.caching;

import static org.geoserver.cloud.catalog.caching.CachingCatalogFacade.DEFAULT_NAMESPACE_CACHE_KEY;
import static org.geoserver.cloud.catalog.caching.CachingCatalogFacade.DEFAULT_WORKSPACE_CACHE_KEY;
import static org.geoserver.cloud.catalog.caching.CachingCatalogFacade.generateDefaultDataStoreKey;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.cloud.bus.event.RemoteInfoEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogInfoModifyEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogInfoRemoveEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteDefaultDataStoreEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteDefaultNamespaceEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteDefaultWorkspaceEvent;
import org.geoserver.cloud.bus.event.config.RemoteGeoServerInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteGeoServerInfoSetEvent;
import org.geoserver.cloud.bus.event.config.RemoteLoggingInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteLoggingInfoSetEvent;
import org.geoserver.cloud.bus.event.config.RemoteServiceInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteServiceInfoRemoveEvent;
import org.geoserver.cloud.bus.event.config.RemoteSettingsInfoModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteSettingsInfoRemoveEvent;
import org.geoserver.cloud.catalog.caching.CachingCatalogFacade;
import org.geoserver.cloud.catalog.caching.CachingGeoServerFacade;
import org.geoserver.cloud.event.ConfigInfoInfoType;
import org.geoserver.config.GeoServerInfo;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.function.BooleanSupplier;

/**
 * Component to listen to {@link RemoteInfoEvent} based hierarchy of events and evict entries from
 * {@link CachingCatalogFacade} and {@link CachingGeoServerFacade} as required by the event type and
 * the object it refers to.
 */
@Slf4j(topic = "org.geoserver.cloud.bus.incoming.caching")
@RequiredArgsConstructor
public @Service class RemoteEventCacheEvictor {

    private final CachingCatalogFacade catalog;
    private final CachingGeoServerFacade config;

    @EventListener(classes = {RemoteDefaultWorkspaceEvent.class})
    public void onSetDefaultWorkspaceEvent(RemoteDefaultWorkspaceEvent event) {
        evictEntry(event, () -> catalog.evict(DEFAULT_WORKSPACE_CACHE_KEY));
    }

    @EventListener(classes = {RemoteDefaultNamespaceEvent.class})
    public void onSetDefaultNamespaceEvent(RemoteDefaultNamespaceEvent event) {
        evictEntry(event, () -> catalog.evict(DEFAULT_NAMESPACE_CACHE_KEY));
    }

    @EventListener(classes = {RemoteDefaultDataStoreEvent.class})
    public void onSetDefaultDataStoreEvent(RemoteDefaultDataStoreEvent event) {
        evictEntry(
                event,
                () -> {
                    String workspaceId = event.getWorkspaceId();
                    WorkspaceInfo info = ResolvingProxy.create(workspaceId, WorkspaceInfo.class);
                    catalog.evict(generateDefaultDataStoreKey(info));
                    return false;
                });
    }

    @EventListener(classes = {RemoteCatalogInfoRemoveEvent.class})
    public void onCatalogInfoRemoveEvent(RemoteCatalogInfoRemoveEvent event) {
        evictCatalogInfo(event);
    }

    @EventListener(classes = {RemoteCatalogInfoModifyEvent.class})
    public void onCatalogInfoModifyEvent(RemoteCatalogInfoModifyEvent event) {
        evictCatalogInfo(event);
    }

    @EventListener(classes = {RemoteGeoServerInfoModifyEvent.class})
    public void onGeoServerInfoModifyEvent(RemoteGeoServerInfoModifyEvent event) {
        if (!event.isFromSelf()) {
            final boolean isUpdateSequenceEvent = event.isUpdateSequenceEvent();
            if (isUpdateSequenceEvent) {
                applyUpdateSequence((RemoteGeoServerInfoModifyEvent) event);
            } else {
                evictConfigEntry(event);
            }
        }
    }

    @EventListener(classes = {RemoteLoggingInfoModifyEvent.class})
    public void onLoggingInfoModifyEvent(RemoteLoggingInfoModifyEvent event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = {RemoteSettingsInfoModifyEvent.class})
    public void onSettingsInfoModifyEvent(RemoteSettingsInfoModifyEvent event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = {RemoteServiceInfoModifyEvent.class})
    public void onServiceInfoModifyEvent(RemoteServiceInfoModifyEvent event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = {RemoteSettingsInfoRemoveEvent.class})
    public void onSettingsInfoRemoveEvent(RemoteSettingsInfoRemoveEvent event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = {RemoteServiceInfoRemoveEvent.class})
    public void onServiceInfoRemoveEvent(RemoteServiceInfoRemoveEvent event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = {RemoteGeoServerInfoSetEvent.class})
    public void onSetGlobalInfoEvent(RemoteGeoServerInfoSetEvent event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = {RemoteLoggingInfoSetEvent.class})
    public void onSetLoggingInfoEvent(RemoteLoggingInfoSetEvent event) {
        evictConfigEntry(event);
    }

    /**
     * Called when the only change to {@link GeoServerInfo} is its update sequence number, in order
     * to avoid evicting the locally cached object and apply the new update sequence to it instead.
     */
    private void applyUpdateSequence(RemoteGeoServerInfoModifyEvent event) {
        GeoServerInfo global = config.getGlobal();
        long current = global.getUpdateSequence();
        long updateSequence = event.getUpdateSequence();
        if (updateSequence == current) {
            log.debug(
                    "Ignoring update sequence event, local value is up to date ({})",
                    updateSequence);
        } else if (updateSequence > current) {
            log.debug(
                    "Applying update sequence instead of evicting. Old: {}, new: {}",
                    current,
                    updateSequence);
            global.setUpdateSequence(updateSequence);
        } else {
            log.info(
                    "Ignoring update sequence event, current sequence ({}) is bigger than remote event's ({})",
                    current,
                    updateSequence);
        }
    }

    private void evictCatalogInfo(RemoteInfoEvent<?, ?> event) {
        evictEntry(
                event,
                () -> {
                    String objectId = event.getObjectId();
                    ConfigInfoInfoType infoType = event.getInfoType();
                    CatalogInfo info =
                            (CatalogInfo) ResolvingProxy.create(objectId, infoType.getType());
                    return catalog.evict(info);
                });
    }

    public void evictConfigEntry(RemoteInfoEvent<?, ?> event) {
        evictEntry(
                event,
                () -> {
                    String objectId = event.getObjectId();
                    ConfigInfoInfoType infoType = event.getInfoType();
                    Info info = ResolvingProxy.create(objectId, infoType.getType());
                    return config.evict(info);
                });
    }

    private void evictEntry(RemoteInfoEvent<?, ?> event, BooleanSupplier evictor) {
        if (event.isFromSelf()) {
            return;
        }
        boolean evicted = evictor.getAsBoolean();
        String eventName = event.getClass().getSimpleName();
        ConfigInfoInfoType eventSourceType = event.getInfoType();
        String eventSourceId = event.getId();
        if (evicted) {
            log.debug(
                    "Evicted cache entry upon remote event {}[{}({})]",
                    eventName,
                    eventSourceType,
                    eventSourceId);
        } else {
            log.trace(
                    "Remote event {}[{}({})] resulted in no cache evict",
                    eventName,
                    eventSourceType,
                    eventSourceId);
        }
    }
}
