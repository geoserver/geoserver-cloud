/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.datadirectory;

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
import org.geoserver.cloud.event.catalog.DefaultDataStoreEvent;
import org.geoserver.cloud.event.catalog.DefaultNamespaceEvent;
import org.geoserver.cloud.event.catalog.DefaultWorkspaceEvent;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoAddEvent;
import org.geoserver.cloud.event.info.InfoPostModifyEvent;
import org.geoserver.cloud.event.info.InfoRemoveEvent;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;
import org.springframework.context.event.EventListener;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Listens to {@link RemoteCatalogEvent}s and updates the local catalog */
@Slf4j(topic = "org.geoserver.cloud.bus.incoming.datadirectory")
@RequiredArgsConstructor
public class DataDirectoryRemoteEventProcessor {

    private final @NonNull RepositoryGeoServerFacade configFacade;
    private final @NonNull ExtendedCatalogFacade catalogFacade;

    @EventListener(InfoRemoveEvent.class)
    public void onRemoteRemoveEvent(InfoRemoveEvent<?, ?, ? extends Info> event) {
        if (event.isLocal()) {
            return;
        }
        final ConfigInfoType type = event.getObjectType();
        final String objectId = event.getObjectId();
        switch (type) {
            case NamespaceInfo:
                remove(objectId, catalogFacade::getNamespace, catalogFacade::remove);
                break;
            case WorkspaceInfo:
                remove(objectId, catalogFacade::getWorkspace, catalogFacade::remove);
                break;
            case CoverageInfo:
            case FeatureTypeInfo:
            case WmsLayerInfo:
            case WmtsLayerInfo:
                remove(
                        objectId,
                        id -> catalogFacade.getResource(id, ResourceInfo.class),
                        catalogFacade::remove);
                break;
            case CoverageStoreInfo:
            case DataStoreInfo:
            case WmsStoreInfo:
            case WmtsStoreInfo:
                remove(
                        objectId,
                        id -> catalogFacade.getStore(id, StoreInfo.class),
                        catalogFacade::remove);
                break;
            case LayerGroupInfo:
                remove(objectId, catalogFacade::getLayerGroup, catalogFacade::remove);
                break;
            case LayerInfo:
                remove(objectId, catalogFacade::getLayer, catalogFacade::remove);
                break;
            case StyleInfo:
                remove(objectId, catalogFacade::getStyle, catalogFacade::remove);
                break;
            case ServiceInfo:
                remove(
                        objectId,
                        id -> configFacade.getService(id, ServiceInfo.class),
                        configFacade::remove);
                break;
            case SettingsInfo:
                remove(objectId, configFacade::getSettings, configFacade::remove);
                break;
            default:
                log.warn("Don't know how to handle remote remove envent for {}", event);
                break;
        }
    }

    @EventListener(InfoAddEvent.class)
    public void onRemoteAddEvent(InfoAddEvent<?, ?, ? extends Info> event) {
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
            case NamespaceInfo:
                catalogFacade.add((NamespaceInfo) object);
                break;
            case WorkspaceInfo:
                catalogFacade.add((WorkspaceInfo) object);
                break;
            case CoverageInfo:
            case FeatureTypeInfo:
            case WmsLayerInfo:
            case WmtsLayerInfo:
                catalogFacade.add((ResourceInfo) object);
                break;
            case CoverageStoreInfo:
            case DataStoreInfo:
            case WmsStoreInfo:
            case WmtsStoreInfo:
                catalogFacade.add((StoreInfo) object);
                break;
            case LayerGroupInfo:
                catalogFacade.add((LayerGroupInfo) object);
                break;
            case LayerInfo:
                catalogFacade.add((LayerInfo) object);
                break;
            case StyleInfo:
                catalogFacade.add((StyleInfo) object);
                break;
            case ServiceInfo:
                configFacade.add((ServiceInfo) object);
                break;
            case SettingsInfo:
                configFacade.add((SettingsInfo) object);
                break;
            default:
                log.warn("Don't know how to handle remote envent {})", event);
                break;
        }
    }

    @EventListener(DefaultWorkspaceEvent.class)
    public void onRemoteDefaultWorkspaceEvent(DefaultWorkspaceEvent event) {
        if (event.isRemote()) {
            String newId = event.getNewWorkspaceId();
            WorkspaceInfo newDefault = newId == null ? null : catalogFacade.getWorkspace(newId);
            catalogFacade.setDefaultWorkspace(newDefault);
        }
    }

    @EventListener(DefaultNamespaceEvent.class)
    public void onRemoteDefaultNamespaceEvent(DefaultNamespaceEvent event) {
        if (event.isRemote()) {
            String newId = event.getNewNamespaceId();
            NamespaceInfo namespace = newId == null ? null : catalogFacade.getNamespace(newId);
            catalogFacade.setDefaultNamespace(namespace);
        }
    }

    @EventListener(DefaultDataStoreEvent.class)
    public void onRemoteDefaultDataStoreEvent(DefaultDataStoreEvent event) {
        if (event.isRemote()) {
            String workspaceId = event.getWorkspaceId();
            WorkspaceInfo workspace = catalogFacade.getWorkspace(workspaceId);
            String storeId = event.getDefaultDataStoreId();
            DataStoreInfo store =
                    storeId == null ? null : catalogFacade.getStore(storeId, DataStoreInfo.class);
            catalogFacade.setDefaultDataStore(workspace, store);
        }
    }

    @EventListener(InfoPostModifyEvent.class)
    public void onRemoteModifyEvent(InfoPostModifyEvent<?, ?, ? extends Info> event) {
        if (event.isLocal()) {
            return;
        }
        final String objectId = event.getObjectId();
        final ConfigInfoType type = event.getObjectType();
        if (type == ConfigInfoType.Catalog) {
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
            case NamespaceInfo:
                info = catalogFacade.getNamespace(objectId);
                break;
            case WorkspaceInfo:
                info = catalogFacade.getWorkspace(objectId);
                break;
            case CoverageInfo:
            case FeatureTypeInfo:
            case WmsLayerInfo:
            case WmtsLayerInfo:
                info = catalogFacade.getResource(objectId, ResourceInfo.class);
                break;
            case CoverageStoreInfo:
            case DataStoreInfo:
            case WmsStoreInfo:
            case WmtsStoreInfo:
                info = catalogFacade.getStore(objectId, StoreInfo.class);
                break;
            case LayerGroupInfo:
                info = catalogFacade.getLayerGroup(objectId);
                break;
            case LayerInfo:
                info = catalogFacade.getLayer(objectId);
                break;
            case StyleInfo:
                info = catalogFacade.getStyle(objectId);
                break;
            case GeoServerInfo:
                info = configFacade.getGlobal();
                info = ModificationProxy.unwrap(info);
                break;
            case ServiceInfo:
                info = configFacade.getService(objectId, ServiceInfo.class);
                info = ModificationProxy.unwrap(info);
                break;
            case SettingsInfo:
                info = configFacade.getSettings(objectId);
                info = ModificationProxy.unwrap(info);
                break;
            default:
                log.warn("Don't know how to handle remote modify envent {}", event);
                return;
        }
        if (info == null) {
            log.warn("Object not found on local Catalog, can't update upon {}", event);
        } else {
            patch.applyTo(info);
            if (info instanceof CatalogInfo) {
                // going directly through the CatalogFacade does not produce any further event
                this.catalogFacade.update((CatalogInfo) info, patch);
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
