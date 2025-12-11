/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.catalog.cache;

import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYERGROUP;
import static org.geoserver.cloud.event.info.ConfigInfoType.NAMESPACE;
import static org.geoserver.cloud.event.info.ConfigInfoType.STYLE;
import static org.geoserver.cloud.event.info.ConfigInfoType.WORKSPACE;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.forwarding.ForwardingExtendedCatalogFacade;
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.catalog.DefaultDataStoreSet;
import org.geoserver.cloud.event.catalog.DefaultNamespaceSet;
import org.geoserver.cloud.event.catalog.DefaultWorkspaceSet;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;

/**
 * @see CachingCatalogFacadeContainmentSupport
 */
public class CachingCatalogFacade extends ForwardingExtendedCatalogFacade {

    /** Name of the cache used as key to acquire it through {@link CacheManager#getCache(String)} */
    static final String CACHE_NAME = "gs-catalog";

    private final CachingCatalogFacadeContainmentSupport support;

    public CachingCatalogFacade(@NonNull ExtendedCatalogFacade facade, @NonNull Cache cache) {
        this(facade, new CachingCatalogFacadeContainmentSupport(cache));
    }

    @VisibleForTesting
    @SuppressWarnings("java:S5803")
    CachingCatalogFacade(@NonNull ExtendedCatalogFacade facade) {
        this(facade, new CachingCatalogFacadeContainmentSupport());
    }

    CachingCatalogFacade(
            @NonNull ExtendedCatalogFacade facade, @NonNull CachingCatalogFacadeContainmentSupport support) {
        super(facade);
        this.support = support;
    }

    // ///////////////////  Eviction event listeners //////////////////////////

    @EventListener(classes = DefaultWorkspaceSet.class)
    public void onDefaultWorkspaceSet() {
        support.evictDefaultWorkspace();
    }

    @EventListener(classes = DefaultNamespaceSet.class)
    public void onDefaultNamespaceSet() {
        support.evictDefaultNamespace();
    }

    @EventListener(DefaultDataStoreSet.class)
    public void onDefaultDataStoreSet(DefaultDataStoreSet event) {
        support.evictDefaultDataStore(event.getWorkspaceId(), event.getObjectName());
    }

    /**
     * Evict the entry on add if the event is remote, to clear a possibly null-value cached. If the
     * event is local, the add(xxx) method shall have overridden it with a cache put
     */
    @EventListener(CatalogInfoAdded.class)
    public void onCatalogInfoAdded(CatalogInfoAdded event) {
        if (event.isRemote()) {
            support.evict(event.getObjectId(), event.getObjectName(), event.getObjectType());
        }
    }

    /**
     * Evicts the cached entries the object id and for both the {@link
     * CatalogInfoModified#getObjectName() new} and {@link CatalogInfoModified#getOldName() old}
     * names.
     */
    @EventListener(CatalogInfoModified.class)
    public void onCatalogInfoModified(CatalogInfoModified event) {
        if (event.isRemote()) {
            support.evict(event.getObjectId(), event.getObjectName(), event.getObjectType());
            support.evict(event.getObjectId(), event.getOldName(), event.getObjectType());
        }
    }

    @EventListener(CatalogInfoRemoved.class)
    public void onCatalogInfoRemovedEvent(CatalogInfoRemoved event) {
        if (event.isRemote()) {
            support.evict(event.getObjectId(), event.getObjectName(), event.getObjectType());
        }
    }

    // ///////////////////  CatalogFacade override methods //////////////////////////

    @Override
    public <T extends CatalogInfo> T add(@NonNull T info) {
        return support.evictAndGet(info, () -> super.add(info));
    }

    @Override
    public void remove(@NonNull CatalogInfo info) {
        support.evict(info);
        super.remove(info);
    }

    @Override
    public <I extends CatalogInfo> I update(@NonNull final I info, @NonNull final Patch patch) {
        // evict new name key in case it's been queried and cached as null before
        evictNewName(info, patch);
        return support.evictAndGet(info, () -> super.update(info, patch));
    }

    private <I extends CatalogInfo> void evictNewName(final I info, final Patch patch) {
        newPrefixedName(info, patch).ifPresent(newPrefixexName -> {
            var type = ConfigInfoType.valueOf(info);
            support.evict(info.getId(), newPrefixexName, type);
        });
    }

    Optional<String> newPrefixedName(CatalogInfo info, Patch patch) {
        return patch.get("name")
                .or(() -> patch.get("prefix"))
                .map(Patch.Property::getValue)
                .map(String::valueOf)
                .map(newName -> InfoNameKey.valueOf(info).withLocalName(newName).prefixexName());
    }

    @Override
    public WorkspaceInfo getWorkspace(@NonNull String id) {
        return support.get(id(id, WORKSPACE), () -> super.getWorkspace(id));
    }

    @Override
    public WorkspaceInfo getWorkspaceByName(@NonNull String name) {
        return support.get(name(name, WORKSPACE), () -> super.getWorkspaceByName(name));
    }

    @Override
    public NamespaceInfo getNamespace(@NonNull String id) {
        return support.get(id(id, NAMESPACE), () -> super.getNamespace(id));
    }

    @Override
    public NamespaceInfo getNamespaceByPrefix(@NonNull String name) {
        return support.get(name(name, NAMESPACE), () -> super.getNamespaceByPrefix(name));
    }

    @Override
    public <T extends StoreInfo> T getStore(@NonNull String id, @NonNull Class<T> clazz) {
        InfoIdKey key = InfoIdKey.valueOf(id, clazz);
        return support.get(key, () -> super.getStore(id, clazz));
    }

    @Override
    public <T extends ResourceInfo> T getResource(@NonNull String id, @NonNull Class<T> clazz) {
        InfoIdKey key = InfoIdKey.valueOf(id, clazz);
        return support.get(key, () -> super.getResource(id, clazz));
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(
            @NonNull NamespaceInfo namespace, @NonNull String name, @NonNull Class<T> clazz) {

        if (CatalogFacade.ANY_NAMESPACE == namespace) {
            return super.getResourceByName(namespace, name, clazz);
        }
        InfoNameKey key = InfoNameKey.valueOf(namespace, name, clazz);
        return support.get(key, () -> super.getResourceByName(namespace, name, clazz));
    }

    @Override
    public StyleInfo getStyle(@NonNull String id) {
        return support.get(id(id, STYLE), () -> super.getStyle(id));
    }

    @Override
    public StyleInfo getStyleByName(@NonNull String name) {
        return support.get(name(name, STYLE), () -> super.getStyleByName(name));
    }

    @Override
    public StyleInfo getStyleByName(@NonNull WorkspaceInfo workspace, @NonNull String name) {
        if (NO_WORKSPACE == workspace || ANY_WORKSPACE == workspace) {
            return super.getStyleByName(workspace, name);
        }

        return support.get(InfoNameKey.valueOf(workspace, name, STYLE), () -> super.getStyleByName(workspace, name));
    }

    @Override
    public LayerInfo getLayer(@NonNull String id) {
        return support.get(id(id, LAYER), () -> super.getLayer(id));
    }

    @Override
    public LayerInfo getLayerByName(@NonNull String name) {
        return support.get(name(name, LAYER), () -> super.getLayerByName(name));
    }

    @Override
    public List<LayerInfo> getLayers(@NonNull ResourceInfo resource) {
        return support.getLayersByResource(resource.getId(), () -> super.getLayers(resource));
    }

    @Override
    public LayerGroupInfo getLayerGroup(@NonNull String id) {
        return support.get(id(id, LAYERGROUP), () -> super.getLayerGroup(id));
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(@NonNull String name) {
        return support.get(name(name, LAYERGROUP), () -> super.getLayerGroupByName(name));
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(@NonNull WorkspaceInfo workspace, @NonNull String name) {
        if (NO_WORKSPACE == workspace || ANY_WORKSPACE == workspace) {
            return super.getLayerGroupByName(workspace, name);
        }

        InfoNameKey key = InfoNameKey.valueOf(workspace, name, LAYERGROUP);
        return support.get(key, () -> super.getLayerGroupByName(workspace, name));
    }

    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return support.getDefaultWorkspace(super::getDefaultWorkspace);
    }

    @Override
    public void setDefaultWorkspace(@Nullable WorkspaceInfo workspace) {
        support.evictDefaultWorkspace();
        super.setDefaultWorkspace(workspace);
    }

    @Override
    public NamespaceInfo getDefaultNamespace() {
        return support.getDefaultNamespace(super::getDefaultNamespace);
    }

    @Override
    public void setDefaultNamespace(@Nullable NamespaceInfo defaultNamespace) {
        support.evictDefaultNamespace();
        super.setDefaultNamespace(defaultNamespace);
    }

    @Override
    public DataStoreInfo getDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        return support.getDefaultDataStore(workspace, () -> super.getDefaultDataStore(workspace));
    }

    @Override
    public void setDefaultDataStore(@NonNull WorkspaceInfo workspace, @Nullable DataStoreInfo store) {
        support.evictDefaultDataStore(workspace);
        super.setDefaultDataStore(workspace, store);
    }

    private static InfoNameKey name(@NonNull String name, @NonNull ConfigInfoType type) {
        return InfoNameKey.valueOf(name, type);
    }

    private static InfoIdKey id(@NonNull String id, @NonNull ConfigInfoType type) {
        return InfoIdKey.valueOf(id, type);
    }

    public void evictAll() {
        support.evictAll();
    }
}
