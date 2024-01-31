/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.remote.cache;

import static org.geoserver.cloud.catalog.cache.CachingCatalogFacade.DEFAULT_NAMESPACE_CACHE_KEY;
import static org.geoserver.cloud.catalog.cache.CachingCatalogFacade.DEFAULT_WORKSPACE_CACHE_KEY;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.cloud.catalog.cache.CachingCatalogFacade;
import org.geoserver.cloud.catalog.cache.CachingGeoServerFacade;
import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.catalog.DefaultDataStoreSet;
import org.geoserver.cloud.event.catalog.DefaultNamespaceSet;
import org.geoserver.cloud.event.catalog.DefaultWorkspaceSet;
import org.geoserver.cloud.event.config.GeoServerInfoModified;
import org.geoserver.cloud.event.config.GeoServerInfoSet;
import org.geoserver.cloud.event.config.LoggingInfoModified;
import org.geoserver.cloud.event.config.LoggingInfoSet;
import org.geoserver.cloud.event.config.ServiceModified;
import org.geoserver.cloud.event.config.ServiceRemoved;
import org.geoserver.cloud.event.config.SettingsModified;
import org.geoserver.cloud.event.config.SettingsRemoved;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.config.GeoServerInfo;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.function.BooleanSupplier;

/**
 * Component to listen to {@link RemoteInfoEvent} based hierarchy of events and evict entries from
 * {@link CachingCatalogFacade} and {@link CachingGeoServerFacade} as required by the event type and
 * the object it refers to.
 */
@Slf4j(topic = "org.geoserver.cloud.event.remote.cache")
@RequiredArgsConstructor
public @Service class RemoteEventCacheEvictor {

    private final CachingCatalogFacade catalog;
    private final CachingGeoServerFacade config;

    @EventListener(classes = {UpdateSequenceEvent.class})
    public void onUpdateSequenceEvent(UpdateSequenceEvent updateSequenceEvent) {
        final Long updateSequence = updateSequenceEvent.getUpdateSequence();
        updateSequenceEvent.remote().ifPresent(remote -> applyUpdateSequence(updateSequence));
    }

    @EventListener(classes = {DefaultWorkspaceSet.class})
    public void onSetDefaultWorkspaceEvent(DefaultWorkspaceSet event) {
        evictEntry(event, () -> catalog.evict(DEFAULT_WORKSPACE_CACHE_KEY));
    }

    @EventListener(classes = {DefaultNamespaceSet.class})
    public void onSetDefaultNamespaceEvent(DefaultNamespaceSet event) {
        evictEntry(event, () -> catalog.evict(DEFAULT_NAMESPACE_CACHE_KEY));
    }

    @EventListener(classes = {DefaultDataStoreSet.class})
    public void onSetDefaultDataStoreEvent(DefaultDataStoreSet event) {
        evictEntry(
                event,
                () -> {
                    String workspaceId = event.getWorkspaceId();
                    WorkspaceInfo workspace =
                            ResolvingProxy.create(workspaceId, WorkspaceInfo.class);
                    catalog.evict(CachingCatalogFacade.generateDefaultDataStoreKey(workspace));
                    return false;
                });
    }

    @EventListener(classes = {CatalogInfoRemoved.class})
    public void onCatalogInfoRemoveEvent(CatalogInfoRemoved event) {
        evictCatalogInfo(event);
    }

    @EventListener(classes = {CatalogInfoModified.class})
    public void onCatalogInfoModifyEvent(CatalogInfoModified event) {
        if (CatalogInfoModified.class.equals(event.getClass())) {
            evictCatalogInfo(event);
        }
    }

    @EventListener(classes = {GeoServerInfoModified.class})
    public void onGeoServerInfoModifyEvent(GeoServerInfoModified event) {
        if (GeoServerInfoModified.class.equals(event.getClass())) {
            evictConfigEntry(event);
        }
    }

    @EventListener(classes = {LoggingInfoModified.class})
    public void onLoggingInfoModifyEvent(LoggingInfoModified event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = {SettingsModified.class})
    public void onSettingsInfoModifyEvent(SettingsModified event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = {ServiceModified.class})
    public void onServiceInfoModifyEvent(ServiceModified event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = {SettingsRemoved.class})
    public void onSettingsInfoRemoveEvent(SettingsRemoved event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = {ServiceRemoved.class})
    public void onServiceInfoRemoveEvent(ServiceRemoved event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = {GeoServerInfoSet.class})
    public void onSetGlobalInfoEvent(GeoServerInfoSet event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = {LoggingInfoSet.class})
    public void onSetLoggingInfoEvent(LoggingInfoSet event) {
        evictConfigEntry(event);
    }

    /**
     * Called when the only change to {@link GeoServerInfo} is its update sequence number, in order
     * to avoid evicting the locally cached object and apply the new update sequence to it instead.
     */
    private void applyUpdateSequence(Long updateSequence) {
        config.evictGlobal()
                .ifPresent(
                        gsinfo ->
                                log.debug(
                                        """
                    Evicted cached GeoServerInfo with updateSequence {} \
                    upon remote event carrying new value {}
                    """,
                                        gsinfo.getUpdateSequence(),
                                        updateSequence));
    }

    private void evictCatalogInfo(InfoEvent event) {
        evictEntry(
                event,
                () -> {
                    String objectId = event.getObjectId();
                    ConfigInfoType infoType = event.getObjectType();
                    CatalogInfo info =
                            (CatalogInfo) ResolvingProxy.create(objectId, infoType.getType());
                    return catalog.evict(info);
                });
    }

    public void evictConfigEntry(InfoEvent event) {
        evictEntry(
                event,
                () -> {
                    String objectId = event.getObjectId();
                    ConfigInfoType infoType = event.getObjectType();
                    Info info = ResolvingProxy.create(objectId, infoType.getType());
                    return config.evict(info);
                });
    }

    private void evictEntry(InfoEvent event, BooleanSupplier evictor) {
        event.remote()
                .ifPresent(
                        evt -> {
                            boolean evicted = evictor.getAsBoolean();
                            if (evicted) {
                                log.debug("Evicted Catalog cache entry due to {}", evt);
                            } else {
                                log.trace("Remote event resulted in no cache eviction: {}", evt);
                            }
                        });
    }
}
