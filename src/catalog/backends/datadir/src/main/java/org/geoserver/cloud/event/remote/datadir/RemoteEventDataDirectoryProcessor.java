/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.remote.datadir;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
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
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;
import org.springframework.context.event.EventListener;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Listens to {@link RemoteCatalogEvent}s and updates the local catalog */
@Slf4j(topic = "org.geoserver.cloud.event.remote.datadir")
@RequiredArgsConstructor
public class RemoteEventDataDirectoryProcessor {

    private final @NonNull RepositoryGeoServerFacade configFacade;
    private final @NonNull ExtendedCatalogFacade catalogFacade;

    @EventListener(classes = {UpdateSequenceEvent.class})
    public void onUpdateSequenceEvent(UpdateSequenceEvent updateSequenceEvent) {
        if (updateSequenceEvent.isRemote()) {
            final Long updateSequence = updateSequenceEvent.getUpdateSequence();
            log.debug("processing update sequence {} from {}", updateSequence, updateSequenceEvent);
            GeoServerInfo info = ModificationProxy.unwrap(configFacade.getGlobal());
            long current = info.getUpdateSequence();
            info.setUpdateSequence(updateSequence);
            log.info("replaced update sequence {} by {}", current, updateSequence);
        }
    }

    @EventListener(InfoRemoved.class)
    public void onRemoteRemoveEvent(InfoRemoved event) {
        if (event.isLocal()) {
            return;
        }
        final ConfigInfoType type = event.getObjectType();
        final String objectId = event.getObjectId();
        switch (type) {
            case NAMESPACE:
                remove(objectId, catalogFacade::getNamespace, catalogFacade::remove);
                break;
            case WORKSPACE:
                remove(objectId, catalogFacade::getWorkspace, catalogFacade::remove);
                break;
            case COVERAGE, FEATURETYPE, WMSLAYER, WMTSLAYER:
                remove(
                        objectId,
                        id -> catalogFacade.getResource(id, ResourceInfo.class),
                        catalogFacade::remove);
                break;
            case COVERAGESTORE, DATASTORE, WMSSTORE, WMTSSTORE:
                remove(
                        objectId,
                        id -> catalogFacade.getStore(id, StoreInfo.class),
                        catalogFacade::remove);
                break;
            case LAYERGROUP:
                remove(objectId, catalogFacade::getLayerGroup, catalogFacade::remove);
                break;
            case LAYER:
                remove(objectId, catalogFacade::getLayer, catalogFacade::remove);
                break;
            case STYLE:
                remove(objectId, catalogFacade::getStyle, catalogFacade::remove);
                break;
            case SERVICE:
                remove(
                        objectId,
                        id -> configFacade.getService(id, ServiceInfo.class),
                        configFacade::remove);
                break;
            case SETTINGS:
                remove(objectId, configFacade::getSettings, configFacade::remove);
                break;
            default:
                log.warn("Don't know how to handle remote remove envent for {}", event);
                break;
        }
    }

    @EventListener(InfoAdded.class)
    public void onRemoteAddEvent(InfoAdded<? extends Info> event) {
        if (event.isLocal()) {
            return;
        }
        final String objectId = event.getObjectId();
        final ConfigInfoType type = event.getObjectType();
        log.debug("Handling remote add event {}({})", type, objectId);
        final Info object = event.getObject();
        if (object == null) {
            log.error("Remote add event didn't send the object payload for {}({})", type, objectId);
            return;
        }
        switch (type) {
            case NAMESPACE:
                catalogFacade.add((NamespaceInfo) object);
                break;
            case WORKSPACE:
                catalogFacade.add((WorkspaceInfo) object);
                break;
            case COVERAGE, FEATURETYPE, WMSLAYER, WMTSLAYER:
                catalogFacade.add((ResourceInfo) object);
                break;
            case COVERAGESTORE, DATASTORE, WMSSTORE, WMTSSTORE:
                catalogFacade.add((StoreInfo) object);
                break;
            case LAYERGROUP:
                catalogFacade.add((LayerGroupInfo) object);
                break;
            case LAYER:
                catalogFacade.add((LayerInfo) object);
                break;
            case STYLE:
                catalogFacade.add((StyleInfo) object);
                break;
            case SERVICE:
                configFacade.add((ServiceInfo) object);
                break;
            case SETTINGS:
                configFacade.add((SettingsInfo) object);
                break;
            case LOGGING:
                // ignore
                break;
            default:
                log.warn("Don't know how to handle remote envent {})", event);
                break;
        }
    }

    @EventListener(DefaultWorkspaceSet.class)
    public void onRemoteDefaultWorkspaceEvent(DefaultWorkspaceSet event) {
        if (event.isRemote()) {
            String newId = event.getNewWorkspaceId();
            WorkspaceInfo newDefault = newId == null ? null : catalogFacade.getWorkspace(newId);
            catalogFacade.setDefaultWorkspace(newDefault);
        }
    }

    @EventListener(DefaultNamespaceSet.class)
    public void onRemoteDefaultNamespaceEvent(DefaultNamespaceSet event) {
        if (event.isRemote()) {
            String newId = event.getNewNamespaceId();
            NamespaceInfo namespace = newId == null ? null : catalogFacade.getNamespace(newId);
            catalogFacade.setDefaultNamespace(namespace);
        }
    }

    @EventListener(DefaultDataStoreSet.class)
    public void onRemoteDefaultDataStoreEvent(DefaultDataStoreSet event) {
        if (event.isRemote()) {
            String workspaceId = event.getWorkspaceId();
            WorkspaceInfo workspace = catalogFacade.getWorkspace(workspaceId);
            String storeId = event.getDefaultDataStoreId();
            DataStoreInfo store =
                    storeId == null ? null : catalogFacade.getStore(storeId, DataStoreInfo.class);
            catalogFacade.setDefaultDataStore(workspace, store);
        }
    }

    @EventListener(InfoModified.class)
    public void onRemoteModifyEvent(InfoModified event) {
        if (event.isLocal()) {
            return;
        }
        final String objectId = event.getObjectId();
        final ConfigInfoType type = event.getObjectType();
        if (type == ConfigInfoType.CATALOG) {
            log.trace(
                    "remote catalog modify events handled by RemoteDefaultWorkspace/Namespace/Store event handlers");
            return;
        }
        log.debug("Handling remote modify event {}", event);
        final Patch patch = event.getPatch();
        if (patch == null) {
            log.error("Remote event didn't send the patch payload {}", event);
            return;
        }
        Info info = null;
        switch (type) {
            case NAMESPACE:
                info = catalogFacade.getNamespace(objectId);
                break;
            case WORKSPACE:
                info = catalogFacade.getWorkspace(objectId);
                break;
            case COVERAGE, FEATURETYPE, WMSLAYER, WMTSLAYER:
                info = catalogFacade.getResource(objectId, ResourceInfo.class);
                break;
            case COVERAGESTORE, DATASTORE, WMSSTORE, WMTSSTORE:
                info = catalogFacade.getStore(objectId, StoreInfo.class);
                break;
            case LAYERGROUP:
                info = catalogFacade.getLayerGroup(objectId);
                break;
            case LAYER:
                info = catalogFacade.getLayer(objectId);
                break;
            case STYLE:
                info = catalogFacade.getStyle(objectId);
                break;
            case GEOSERVER:
                info = ModificationProxy.unwrap(configFacade.getGlobal());
                break;
            case SERVICE:
                info =
                        ModificationProxy.unwrap(
                                configFacade.getService(objectId, ServiceInfo.class));
                break;
            case SETTINGS:
                info = ModificationProxy.unwrap(configFacade.getSettings(objectId));
                break;
            case LOGGING:
                info = ModificationProxy.unwrap(configFacade.getLogging());
                break;
            default:
                log.warn("Don't know how to handle remote modify envent {}", event);
                return;
        }
        if (info == null) {
            log.warn("Object not found on local Catalog, can't update upon {}", event);
        } else {
            patch.applyTo(info);
            if (info instanceof CatalogInfo catalogInfo) {
                // going directly through the CatalogFacade does not produce any further event
                this.catalogFacade.update(catalogInfo, patch);
            }
            log.debug(
                    "Object updated: {}({}). Properties: {}",
                    type,
                    objectId,
                    patch.getPropertyNames().stream().collect(Collectors.joining(",")));
        }
    }

    private <T extends Info> void remove(
            String id, Function<String, T> supplier, Consumer<? super T> remover) {
        T info = supplier.apply(id);
        if (info == null) {
            log.warn("Can't remove {}, not present in local catalog", id);
        } else {
            remover.accept(info);
            log.debug("Removed {}", id);
        }
    }
}
