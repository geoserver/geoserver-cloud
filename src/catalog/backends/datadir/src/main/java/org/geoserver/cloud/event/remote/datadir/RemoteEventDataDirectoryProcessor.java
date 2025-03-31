/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.remote.datadir;

import java.util.function.Consumer;
import java.util.function.Function;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.geoserver.cloud.event.catalog.DefaultDataStoreSet;
import org.geoserver.cloud.event.catalog.DefaultNamespaceSet;
import org.geoserver.cloud.event.catalog.DefaultWorkspaceSet;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoAdded;
import org.geoserver.cloud.event.info.InfoModified;
import org.geoserver.cloud.event.info.InfoRemoved;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;
import org.springframework.context.event.EventListener;

/**
 * Listens to {@link RemoteCatalogEvent}s and updates the local catalog
 *
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.event.remote.datadir")
@RequiredArgsConstructor
class RemoteEventDataDirectoryProcessor {

    private final @NonNull RepositoryGeoServerFacade configFacade;
    private final @NonNull CatalogPlugin rawCatalog;

    ExtendedCatalogFacade catalogFacade() {
        return rawCatalog.getFacade();
    }

    @EventListener(UpdateSequenceEvent.class)
    void onRemoteUpdateSequenceEvent(UpdateSequenceEvent event) {
        if (event.isLocal()) {
            return;
        }
        final long updateSequence = event.getUpdateSequence();
        GeoServerInfo info = ModificationProxy.unwrap(configFacade.getGlobal());
        if (null == info) return;
        final long current = info.getUpdateSequence();
        if (updateSequence > current) {
            info.setUpdateSequence(updateSequence);
            log.debug(
                    "replaced update sequence {} by {} due to reomote event {}",
                    current,
                    updateSequence,
                    event.toShortString());
        } else {
            log.debug(
                    "remote event has update sequence {} lower than locally seen {}, leaving it untouched. {}",
                    updateSequence,
                    current,
                    event.toShortString());
        }
    }

    @EventListener(InfoRemoved.class)
    public void onRemoteRemoveEvent(InfoRemoved event) {
        if (event.isLocal()) {
            return;
        }
        final ConfigInfoType type = event.getObjectType();
        final String objectId = event.getObjectId();
        final ExtendedCatalogFacade facade = catalogFacade();

        if (type.isA(CatalogInfo.class)) {
            Class<? extends CatalogInfo> cinfotype = type.type();
            facade.get(objectId, cinfotype)
                    .ifPresentOrElse(
                            info -> {
                                facade.remove(info);
                                log.debug("Removed {}({}), from local catalog", type, objectId);
                            },
                            () -> log.warn("Can't remove {}({}), not present in local catalog", type, objectId));
        } else {
            boolean removed =
                    switch (type) {
                        case SERVICE ->
                            remove(
                                    objectId,
                                    id -> configFacade.getService(id, ServiceInfo.class),
                                    configFacade::remove);
                        case SETTINGS -> remove(objectId, configFacade::getSettings, configFacade::remove);
                        default -> false;
                    };
            if (removed) {
                log.debug("Removed {}({}), from local config", type, objectId);
            } else {
                log.warn("Can't remove {}({}), not present in local config", type, objectId);
            }
        }
    }

    @EventListener(InfoAdded.class)
    public void onRemoteAddEvent(InfoAdded<? extends Info> event) {
        if (event.isLocal()) {
            return;
        }
        final String objectId = event.getObjectId();
        final ConfigInfoType type = event.getObjectType();
        final Info object = event.getObject();
        if (object == null) {
            log.error("Remote add event didn't send the object payload for {}({})", type, objectId);
            return;
        }
        if (log.isDebugEnabled()) log.debug("Adding object from event {}: {}", event.toShortString(), object);
        ExtendedCatalogFacade facade = catalogFacade();
        switch (object) {
            case CatalogInfo info -> facade.add(info);
            case ServiceInfo config -> configFacade.add(config);
            case SettingsInfo config -> configFacade.add(config);
            case LoggingInfo config -> log.debug("ignoring unused LoggingInfo {}", config);
            default -> log.warn("Don't know how to handle remote envent {})", event);
        }
        if (log.isDebugEnabled()) log.debug("Added object from event {}: {}", event.toShortString(), object);
    }

    @EventListener(InfoModified.class)
    public void onRemoteModifyEvent(InfoModified event) {
        if (event.isLocal()) {
            return;
        }
        if (event instanceof DefaultWorkspaceSet
                || event instanceof DefaultNamespaceSet
                || event instanceof DefaultDataStoreSet
                || ConfigInfoType.CATALOG.equals(event.getObjectType())) {
            // these are InfoModified events but have their own listeners
            return;
        }
        final ConfigInfoType type = event.getObjectType();
        final Patch patch = event.getPatch();
        if (patch == null) {
            log.error("Remote event didn't send the patch payload {}", event);
            return;
        }
        log.debug("Handling remote modify event {}", event);
        Info info = loadInfo(event);
        if (info == null) {
            log.warn("Object not found on local Catalog, can't update upon {}", event);
            return;
        }
        if (info instanceof CatalogInfo catalogInfo) {
            // going directly through the CatalogFacade does not produce any further event
            this.catalogFacade().update(catalogInfo, patch);
        } else {
            // config info. GeoServerFacade doesn't have an update(info, patch) method, apply
            // the patch to the live object
            info = ModificationProxy.unwrap(info);
            patch.applyTo(info);
        }

        if (log.isDebugEnabled())
            log.debug("Object updated: {}({}). Properties: {}", type, event.getObjectId(), patch.getPropertyNames());
    }

    private Info loadInfo(InfoModified event) {
        final ConfigInfoType type = event.getObjectType();
        final String objectId = event.getObjectId();
        if (type.isA(CatalogInfo.class)) {
            @SuppressWarnings("unchecked")
            Class<? extends CatalogInfo> ctype = (Class<? extends CatalogInfo>) type.getType();
            return catalogFacade().get(objectId, ctype).orElse(null);
        }
        return switch (type) {
            case GEOSERVER -> configFacade.getGlobal();
            case SERVICE -> configFacade.getService(objectId, ServiceInfo.class);
            case SETTINGS -> configFacade.getSettings(objectId);
            case LOGGING -> configFacade.getLogging();
            default -> {
                log.warn("Don't know how to handle remote modify envent {}", event);
                yield null;
            }
        };
    }

    @EventListener(DefaultWorkspaceSet.class)
    public void onRemoteDefaultWorkspaceEvent(DefaultWorkspaceSet event) {
        if (event.isLocal()) {
            return;
        }

        WorkspaceInfo newDefault = null;
        if (null != event.getNewWorkspaceId()) {
            // let the facade handle the resolving and eventual consistency
            newDefault = ResolvingProxy.create(event.getNewWorkspaceId(), WorkspaceInfo.class);
        }
        catalogFacade().setDefaultWorkspace(newDefault);
    }

    @EventListener(DefaultNamespaceSet.class)
    public void onRemoteDefaultNamespaceEvent(DefaultNamespaceSet event) {
        if (event.isLocal()) {
            return;
        }
        NamespaceInfo namespace = null;
        if (null != event.getNewNamespaceId()) {
            // let the facade handle the resolving and eventual consistency
            namespace = ResolvingProxy.create(event.getNewNamespaceId(), NamespaceInfo.class);
        }
        catalogFacade().setDefaultNamespace(namespace);
    }

    @EventListener(DefaultDataStoreSet.class)
    public void onRemoteDefaultDataStoreEvent(DefaultDataStoreSet event) {
        if (event.isLocal()) {
            return;
        }

        ExtendedCatalogFacade facade = catalogFacade();

        WorkspaceInfo workspace = ResolvingProxy.create(event.getWorkspaceId(), WorkspaceInfo.class);

        DataStoreInfo store = null;
        if (null != event.getDefaultDataStoreId()) {
            store = ResolvingProxy.create(event.getDefaultDataStoreId(), DataStoreInfo.class);
        }
        facade.setDefaultDataStore(workspace, store);
    }

    private <T extends Info> boolean remove(String id, Function<String, T> supplier, Consumer<? super T> remover) {
        T info = supplier.apply(id);
        if (info == null) {
            return false;
        }
        remover.accept(info);
        return true;
    }
}
