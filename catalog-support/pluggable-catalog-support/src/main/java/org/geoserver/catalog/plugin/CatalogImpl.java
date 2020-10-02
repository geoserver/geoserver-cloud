/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogCapabilities;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LockingCatalogFacade;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogBeforeAddEvent;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.event.impl.CatalogAddEventImpl;
import org.geoserver.catalog.event.impl.CatalogBeforeAddEventImpl;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogPostModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogRemoveEventImpl;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geoserver.catalog.impl.CoverageDimensionImpl;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.DefaultCatalogFacade;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.catalog.impl.WMTSLayerInfoImpl;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/** */
@SuppressWarnings("serial")
public class CatalogImpl implements Catalog {

    /** logger */
    private static final Logger LOGGER = Logging.getLogger(CatalogImpl.class);

    /** data access facade */
    protected CatalogFacade facade;

    /** listeners */
    protected List<CatalogListener> listeners = new CopyOnWriteArrayList<>();

    /** resources */
    protected ResourcePool resourcePool;

    protected GeoServerResourceLoader resourceLoader;

    /** Handles {@link CatalogInfo} validation rules before adding or updating an object */
    protected final CatalogValidationRules validationSupport;

    public CatalogImpl() {
        facade = new DefaultCatalogFacade(this);
        // wrap the default catalog facade with the facade capable of handling isolated workspaces
        // behavior
        facade = new IsolatedCatalogFacade(facade);
        setFacade(facade);
        resourcePool = ResourcePool.create(this);
        validationSupport = new CatalogValidationRules(this);
    }

    public @Override CatalogFacade getFacade() {
        return facade;
    }

    /**
     * Turn on/off extended validation switch.
     *
     * <p>This is not part of the public api, it is used for testing purposes where we have to
     * bootstrap catalog contents.
     */
    public void setExtendedValidation(boolean extendedValidation) {
        validationSupport.setExtendedValidation(extendedValidation);
    }

    public boolean isExtendedValidation() {
        return validationSupport.isExtendedValidation();
    }

    public void setFacade(CatalogFacade facade) {
        final GeoServerConfigurationLock configurationLock =
                GeoServerExtensions.bean(GeoServerConfigurationLock.class);
        if (configurationLock != null) {
            facade = LockingCatalogFacade.create(facade, configurationLock);
        }
        this.facade = facade;
        facade.setCatalog(this);
    }

    public @Override String getId() {
        return "catalog";
    }

    public @Override CatalogFactory getFactory() {
        return new CatalogFactoryImpl(this);
    }

    // Store methods
    public @Override void add(StoreInfo store) {

        if (store.getWorkspace() == null) {
            store.setWorkspace(getDefaultWorkspace());
        }

        validate(store, true);

        // TODO: remove synchronized block, need transactions
        StoreInfo added;
        synchronized (facade) {
            StoreInfo resolved = resolve(store);
            beforeadded(resolved);
            added = facade.add(resolved);

            // if there is no default store use this one as the default
            if (getDefaultDataStore(store.getWorkspace()) == null
                    && store instanceof DataStoreInfo) {
                setDefaultDataStore(store.getWorkspace(), (DataStoreInfo) store);
            }
        }
        added(added);
    }

    public @Override ValidationResult validate(StoreInfo store, boolean isNew) {
        return validationSupport.validate(store, isNew);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // setDefaultDataStore allows for null store
    public @Override void remove(StoreInfo store) {
        validationSupport.beforeRemove(store);
        // TODO: remove synchronized block, need transactions
        synchronized (facade) {
            facade.remove(store);

            WorkspaceInfo workspace = store.getWorkspace();
            DataStoreInfo defaultStore = getDefaultDataStore(workspace);
            if (store.equals(defaultStore) || defaultStore == null) {
                // TODO: this will fire multiple events, we want to fire only one
                setDefaultDataStore(workspace, null);

                // default removed, choose another store to become default if possible
                List<DataStoreInfo> dstores = getStoresByWorkspace(workspace, DataStoreInfo.class);
                if (!dstores.isEmpty()) {
                    setDefaultDataStore(workspace, (DataStoreInfo) dstores.get(0));
                }
            }
        }

        removed(store);
    }

    public @Override void save(StoreInfo store) {
        validate(store, false);
        facade.save(store);
    }

    public @Override <T extends StoreInfo> T detach(T store) {
        return detached(store, facade.detach(store));
    }

    public @Override <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return facade.getStore(id, clazz);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public @Override <T extends StoreInfo> T getStoreByName(String name, Class<T> clazz) {
        return getStoreByName((WorkspaceInfo) null, name, clazz);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public @Override <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {

        WorkspaceInfo ws = workspace;
        if (ws == null) {
            ws = getDefaultWorkspace();
        }

        if (clazz != null
                && clazz.isAssignableFrom(DataStoreInfo.class)
                && (name == null || name.equals(Catalog.DEFAULT))) {
            return clazz.cast(getDefaultDataStore(workspace));
        }

        T store = facade.getStoreByName(ws, name, clazz);
        if (store == null && workspace == null) {
            store = facade.getStoreByName(CatalogFacade.ANY_WORKSPACE, name, clazz);
        }
        return store;
    }

    public @Override <T extends StoreInfo> T getStoreByName(
            String workspaceName, String name, Class<T> clazz) {

        WorkspaceInfo workspace = getWorkspaceByName(workspaceName);
        if (workspace != null) {
            return getStoreByName(workspace, name, clazz);
        }
        return null;
    }

    public @Override <T extends StoreInfo> List<T> getStoresByWorkspace(
            String workspaceName, Class<T> clazz) {

        WorkspaceInfo workspace = null;
        if (workspaceName != null) {
            workspace = getWorkspaceByName(workspaceName);
            if (workspace == null) {
                return Collections.emptyList();
            }
        }

        return getStoresByWorkspace(workspace, clazz);
    }

    public @Override <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {

        return facade.getStoresByWorkspace(workspace, clazz);
    }

    public @Override <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return facade.getStores(clazz);
    }

    public @Override DataStoreInfo getDataStore(String id) {
        return (DataStoreInfo) getStore(id, DataStoreInfo.class);
    }

    public @Override DataStoreInfo getDataStoreByName(String name) {
        return (DataStoreInfo) getStoreByName(name, DataStoreInfo.class);
    }

    public @Override DataStoreInfo getDataStoreByName(String workspaceName, String name) {
        return (DataStoreInfo) getStoreByName(workspaceName, name, DataStoreInfo.class);
    }

    public @Override DataStoreInfo getDataStoreByName(WorkspaceInfo workspace, String name) {
        return (DataStoreInfo) getStoreByName(workspace, name, DataStoreInfo.class);
    }

    public @Override List<DataStoreInfo> getDataStoresByWorkspace(String workspaceName) {
        return getStoresByWorkspace(workspaceName, DataStoreInfo.class);
    }

    public @Override List<DataStoreInfo> getDataStoresByWorkspace(WorkspaceInfo workspace) {
        return getStoresByWorkspace(workspace, DataStoreInfo.class);
    }

    public @Override List<DataStoreInfo> getDataStores() {
        return getStores(DataStoreInfo.class);
    }

    public @Override DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return facade.getDefaultDataStore(workspace);
    }

    public @Override void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        if (store != null) {
            // basic sanity check
            if (store.getWorkspace() == null) {
                throw new IllegalArgumentException("The store has not been assigned a workspace");
            }

            if (!store.getWorkspace().equals(workspace)) {
                throw new IllegalArgumentException(
                        "Trying to mark as default "
                                + "for workspace "
                                + workspace.getName()
                                + " a store that "
                                + "is contained in "
                                + store.getWorkspace().getName());
            }
        }
        facade.setDefaultDataStore(workspace, store);
    }

    public @Override CoverageStoreInfo getCoverageStore(String id) {
        return (CoverageStoreInfo) getStore(id, CoverageStoreInfo.class);
    }

    public @Override CoverageStoreInfo getCoverageStoreByName(String name) {
        return (CoverageStoreInfo) getStoreByName(name, CoverageStoreInfo.class);
    }

    public @Override CoverageStoreInfo getCoverageStoreByName(String workspaceName, String name) {
        return getStoreByName(workspaceName, name, CoverageStoreInfo.class);
    }

    public @Override CoverageStoreInfo getCoverageStoreByName(
            WorkspaceInfo workspace, String name) {
        return getStoreByName(workspace, name, CoverageStoreInfo.class);
    }

    public @Override List<CoverageStoreInfo> getCoverageStoresByWorkspace(String workspaceName) {
        return getStoresByWorkspace(workspaceName, CoverageStoreInfo.class);
    }

    public @Override List<CoverageStoreInfo> getCoverageStoresByWorkspace(WorkspaceInfo workspace) {
        return getStoresByWorkspace(workspace, CoverageStoreInfo.class);
    }

    public @Override List<CoverageStoreInfo> getCoverageStores() {
        return getStores(CoverageStoreInfo.class);
    }

    // Resource methods
    public @Override void add(ResourceInfo resource) {
        if (resource.getNamespace() == null) {
            // default to default namespace
            resource.setNamespace(getDefaultNamespace());
        }
        if (resource.getNativeName() == null) {
            resource.setNativeName(resource.getName());
        }
        ResourceInfo resolved = resolve(resource);
        validate(resolved, true);
        beforeadded(resolved);
        ResourceInfo added = facade.add(resolved);
        added(added);
    }

    public @Override ValidationResult validate(ResourceInfo resource, boolean isNew) {
        return validationSupport.validate(resource, isNew);
    }

    public @Override void remove(ResourceInfo resource) {
        validationSupport.beforeRemove(resource);
        facade.remove(resource);
        removed(resource);
    }

    public @Override void save(ResourceInfo resource) {
        validate(resource, false);
        facade.save(resource);
    }

    public @Override <T extends ResourceInfo> T detach(T resource) {
        return detached(resource, facade.detach(resource));
    }

    public @Override <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return facade.getResource(id, clazz);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public @Override <T extends ResourceInfo> T getResourceByName(
            String ns, String name, Class<T> clazz) {
        if ("".equals(ns)) {
            ns = null;
        }

        if (ns != null) {
            NamespaceInfo namespace = getNamespaceByPrefix(ns);
            if (namespace == null) {
                namespace = getNamespaceByURI(ns);
            }

            if (namespace != null) {
                return getResourceByName(namespace, name, clazz);
            }

            return null;
        }

        return getResourceByName((NamespaceInfo) null, name, clazz);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public @Override <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo ns, String name, Class<T> clazz) {

        NamespaceInfo namespace = ns;
        if (namespace == null) {
            namespace = getDefaultNamespace();
        }
        T resource = facade.getResourceByName(namespace, name, clazz);
        if (resource == null && ns == null) {
            resource = facade.getResourceByName(CatalogFacade.ANY_NAMESPACE, name, clazz);
        }
        return resource;
    }

    public @Override <T extends ResourceInfo> T getResourceByName(Name name, Class<T> clazz) {
        return getResourceByName(name.getNamespaceURI(), name.getLocalPart(), clazz);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public @Override <T extends ResourceInfo> T getResourceByName(String name, Class<T> clazz) {
        // check is the name is a fully qualified one
        int colon = name.indexOf(':');
        if (colon != -1) {
            String ns = name.substring(0, colon);
            String localName = name.substring(colon + 1);
            return getResourceByName(ns, localName, clazz);
        } else {
            return getResourceByName((String) null, name, clazz);
        }
    }

    public @Override <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return facade.getResources(clazz);
    }

    public @Override <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        return facade.getResourcesByNamespace(namespace, clazz);
    }

    public @Override <T extends ResourceInfo> List<T> getResourcesByNamespace(
            String namespace, Class<T> clazz) {
        if (namespace == null) {
            return getResourcesByNamespace((NamespaceInfo) null, clazz);
        }

        NamespaceInfo ns = getNamespaceByPrefix(namespace);
        if (ns == null) {
            ns = getNamespaceByURI(namespace);
        }
        if (ns == null) {
            return Collections.emptyList();
        }

        return getResourcesByNamespace(ns, clazz);
    }

    public @Override <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        return facade.getResourceByStore(store, name, clazz);
    }

    public @Override <T extends ResourceInfo> List<T> getResourcesByStore(
            StoreInfo store, Class<T> clazz) {
        return facade.getResourcesByStore(store, clazz);
    }

    public @Override FeatureTypeInfo getFeatureType(String id) {
        return (FeatureTypeInfo) getResource(id, FeatureTypeInfo.class);
    }

    public @Override FeatureTypeInfo getFeatureTypeByName(String ns, String name) {
        return (FeatureTypeInfo) getResourceByName(ns, name, FeatureTypeInfo.class);
    }

    public @Override FeatureTypeInfo getFeatureTypeByName(NamespaceInfo ns, String name) {
        return getResourceByName(ns, name, FeatureTypeInfo.class);
    }

    public @Override FeatureTypeInfo getFeatureTypeByName(Name name) {
        return getResourceByName(name, FeatureTypeInfo.class);
    }

    public @Override FeatureTypeInfo getFeatureTypeByName(String name) {
        return (FeatureTypeInfo) getResourceByName(name, FeatureTypeInfo.class);
    }

    public @Override List<FeatureTypeInfo> getFeatureTypes() {
        return getResources(FeatureTypeInfo.class);
    }

    public @Override List<FeatureTypeInfo> getFeatureTypesByNamespace(NamespaceInfo namespace) {
        return getResourcesByNamespace(namespace, FeatureTypeInfo.class);
    }

    public @Override FeatureTypeInfo getFeatureTypeByDataStore(
            DataStoreInfo dataStore, String name) {
        return getResourceByStore(dataStore, name, FeatureTypeInfo.class);
    }

    public @Override List<FeatureTypeInfo> getFeatureTypesByDataStore(DataStoreInfo store) {
        return getResourcesByStore(store, FeatureTypeInfo.class);
    }

    public @Override CoverageInfo getCoverage(String id) {
        return (CoverageInfo) getResource(id, CoverageInfo.class);
    }

    public @Override CoverageInfo getCoverageByName(String ns, String name) {
        return (CoverageInfo) getResourceByName(ns, name, CoverageInfo.class);
    }

    public @Override CoverageInfo getCoverageByName(NamespaceInfo ns, String name) {
        return (CoverageInfo) getResourceByName(ns, name, CoverageInfo.class);
    }

    public @Override CoverageInfo getCoverageByName(Name name) {
        return getResourceByName(name, CoverageInfo.class);
    }

    public @Override CoverageInfo getCoverageByName(String name) {
        return (CoverageInfo) getResourceByName(name, CoverageInfo.class);
    }

    public @Override List<CoverageInfo> getCoverages() {
        return getResources(CoverageInfo.class);
    }

    public @Override List<CoverageInfo> getCoveragesByNamespace(NamespaceInfo namespace) {
        return getResourcesByNamespace(namespace, CoverageInfo.class);
    }

    public @Override List<CoverageInfo> getCoveragesByStore(CoverageStoreInfo store) {
        return getResourcesByStore(store, CoverageInfo.class);
    }

    public @Override CoverageInfo getCoverageByCoverageStore(
            CoverageStoreInfo coverageStore, String name) {
        return getResourceByStore(coverageStore, name, CoverageInfo.class);
    }

    public @Override List<CoverageInfo> getCoveragesByCoverageStore(CoverageStoreInfo store) {
        return getResourcesByStore(store, CoverageInfo.class);
    }

    // Layer methods
    public @Override void add(LayerInfo layer) {
        layer = resolve(layer);
        validate(layer, true);

        if (layer.getType() == null) {
            if (layer.getResource() instanceof FeatureTypeInfo) {
                layer.setType(PublishedType.VECTOR);
            } else if (layer.getResource() instanceof CoverageInfo) {
                layer.setType(PublishedType.RASTER);
            } else if (layer.getResource() instanceof WMTSLayerInfo) {
                layer.setType(PublishedType.WMTS);
            } else if (layer.getResource() instanceof WMSLayerInfo) {
                layer.setType(PublishedType.WMS);
            } else {
                String msg = "Layer type not set and can't be derived from resource";
                throw new IllegalArgumentException(msg);
            }
        }
        beforeadded(layer);
        LayerInfo added = facade.add(layer);
        added(added);
    }

    public @Override ValidationResult validate(LayerInfo layer, boolean isNew) {
        return validationSupport.validate(layer, isNew);
    }

    public @Override void remove(LayerInfo layer) {
        validationSupport.beforeRemove(layer);
        facade.remove(layer);
        removed(layer);
    }

    public @Override void save(LayerInfo layer) {
        validate(layer, false);
        facade.save(layer);
    }

    public @Override LayerInfo detach(LayerInfo layer) {
        return detached(layer, facade.detach(layer));
    }

    public @Override LayerInfo getLayer(String id) {
        return facade.getLayer(id);
    }

    public @Override LayerInfo getLayerByName(Name name) {
        if (name.getNamespaceURI() != null) {
            NamespaceInfo ns = getNamespaceByURI(name.getNamespaceURI());
            if (ns != null) {
                return getLayerByName(ns.getPrefix() + ":" + name.getLocalPart());
            }
        }

        return getLayerByName(name.getLocalPart());
    }

    public @Override LayerInfo getLayerByName(String name) {
        LayerInfo result = null;
        int colon = name.indexOf(':');
        if (colon != -1) {
            // search by resource name
            String prefix = name.substring(0, colon);
            String resource = name.substring(colon + 1);

            result = getLayerByName(this, prefix, resource);
        } else {
            // search in default workspace first
            WorkspaceInfo ws = getDefaultWorkspace();
            if (ws != null) {
                result = getLayerByName(this, ws.getName(), name);
            }
        }

        if (result == null) {
            result = facade.getLayerByName(name);
        }

        return result;
    }

    static LayerInfo getLayerByName(Catalog catalog, String workspace, String resourceName) {
        ResourceInfo r = catalog.getResourceByName(workspace, resourceName, ResourceInfo.class);
        if (r == null) {
            return null;
        }
        List<LayerInfo> layers = catalog.getLayers(r);
        if (layers.size() == 1) {
            return layers.get(0);
        } else {
            return null;
        }
    }

    public @Override List<LayerInfo> getLayers(ResourceInfo resource) {
        return facade.getLayers(resource);
    }

    public @Override List<LayerInfo> getLayers(StyleInfo style) {
        return facade.getLayers(style);
    }

    public @Override List<LayerInfo> getLayers() {
        return facade.getLayers();
    }

    // Map methods
    public @Override MapInfo getMap(String id) {
        return facade.getMap(id);
    }

    public @Override MapInfo getMapByName(String name) {
        return facade.getMapByName(name);
    }

    public @Override List<MapInfo> getMaps() {
        return facade.getMaps();
    }

    public @Override void add(LayerGroupInfo layerGroup) {
        layerGroup = resolve(layerGroup);
        validate(layerGroup, true);

        List<StyleInfo> styles = layerGroup.getStyles();
        if (styles.isEmpty()) {
            layerGroup.getLayers().forEach(l -> styles.add(null));
        }
        beforeadded(layerGroup);
        LayerGroupInfo added = facade.add(layerGroup);
        added(added);
    }

    public @Override ValidationResult validate(LayerGroupInfo layerGroup, boolean isNew) {
        return validationSupport.validate(layerGroup, isNew);
    }

    public @Override void remove(LayerGroupInfo layerGroup) {
        validationSupport.beforeRemove(layerGroup);
        facade.remove(layerGroup);
        removed(layerGroup);
    }

    public @Override void save(LayerGroupInfo layerGroup) {
        validate(layerGroup, false);
        facade.save(layerGroup);
    }

    public @Override LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return detached(layerGroup, facade.detach(layerGroup));
    }

    public @Override List<LayerGroupInfo> getLayerGroups() {
        return facade.getLayerGroups();
    }

    public @Override List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName) {
        WorkspaceInfo workspace = null;
        if (workspaceName != null) {
            workspace = getWorkspaceByName(workspaceName);
            if (workspace == null) {
                return Collections.emptyList();
            }
        }

        return getLayerGroupsByWorkspace(workspace);
    }

    public @Override List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return facade.getLayerGroupsByWorkspace(workspace);
    }

    public @Override LayerGroupInfo getLayerGroup(String id) {
        return facade.getLayerGroup(id);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public @Override LayerGroupInfo getLayerGroupByName(String name) {

        final LayerGroupInfo layerGroup = getLayerGroupByName((String) null, name);

        if (layerGroup != null) return layerGroup;

        // last chance: checking handle prefixed name case
        String workspaceName = null;
        String layerGroupName = null;

        int colon = name.indexOf(':');
        if (colon == -1) {
            // if there is no prefix, try the default workspace
            WorkspaceInfo defaultWs = getDefaultWorkspace();
            workspaceName = defaultWs == null ? null : defaultWs.getName();
            layerGroupName = name;
        }
        if (colon != -1) {
            workspaceName = name.substring(0, colon);
            layerGroupName = name.substring(colon + 1);
        }

        return getLayerGroupByName(workspaceName, layerGroupName);
    }

    public @Override LayerGroupInfo getLayerGroupByName(String workspaceName, String name) {
        WorkspaceInfo workspace = null;
        if (workspaceName != null) {
            workspace = getWorkspaceByName(workspaceName);
            if (workspace == null) {
                return null;
            }
        }

        return getLayerGroupByName(workspace, name);
    }

    public @Override LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {

        if (null == workspace) {
            workspace = DefaultCatalogFacade.NO_WORKSPACE;
        }

        LayerGroupInfo layerGroup = facade.getLayerGroupByName(workspace, name);
        return layerGroup;
    }

    public @Override void add(MapInfo map) {
        beforeadded(map);
        MapInfo added = facade.add(resolve(map));
        added(added);
    }

    public @Override void remove(MapInfo map) {
        validationSupport.beforeRemove(map);
        facade.remove(map);
        removed(map);
    }

    public @Override void save(MapInfo map) {
        facade.save(map);
    }

    public @Override MapInfo detach(MapInfo map) {
        return detached(map, facade.detach(map));
    }

    // Namespace methods
    public @Override NamespaceInfo getNamespace(String id) {
        return facade.getNamespace(id);
    }

    public @Override NamespaceInfo getNamespaceByPrefix(String prefix) {
        if (prefix == null || Catalog.DEFAULT.equals(prefix)) {
            NamespaceInfo ns = getDefaultNamespace();
            if (ns != null) {
                prefix = ns.getPrefix();
            }
        }

        return facade.getNamespaceByPrefix(prefix);
    }

    public @Override NamespaceInfo getNamespaceByURI(String uri) {
        return facade.getNamespaceByURI(uri);
    }

    public @Override List<NamespaceInfo> getNamespaces() {
        return facade.getNamespaces();
    }

    public @Override void add(NamespaceInfo namespace) {
        validate(namespace, true);

        NamespaceInfo added;
        synchronized (facade) {
            final NamespaceInfo resolved = resolve(namespace);
            beforeadded(namespace);
            added = facade.add(resolved);
            if (getDefaultNamespace() == null) {
                setDefaultNamespace(resolved);
            }
        }

        added(added);
    }

    public @Override ValidationResult validate(NamespaceInfo namespace, boolean isNew) {
        return validationSupport.validate(namespace, isNew);
    }

    @SuppressFBWarnings("NP_NULL_PARAM_DEREF") // I don't see this happening...
    public @Override void remove(NamespaceInfo namespace) {
        validationSupport.beforeRemove(namespace);
        // TODO: remove synchronized block, need transactions
        synchronized (facade) {
            facade.remove(namespace);

            NamespaceInfo defaultNamespace = getDefaultNamespace();
            if (namespace.equals(defaultNamespace) || defaultNamespace == null) {
                List<NamespaceInfo> namespaces = facade.getNamespaces();

                defaultNamespace = null;
                if (!namespaces.isEmpty()) {
                    defaultNamespace = namespaces.get(0);
                }

                setDefaultNamespace(defaultNamespace);
                if (defaultNamespace != null) {
                    WorkspaceInfo defaultWorkspace =
                            getWorkspaceByName(defaultNamespace.getPrefix());
                    if (defaultWorkspace != null) {
                        setDefaultWorkspace(defaultWorkspace);
                    }
                }
            }
        }
        removed(namespace);
    }

    public @Override void save(NamespaceInfo namespace) {
        validate(namespace, false);

        facade.save(namespace);
    }

    public @Override NamespaceInfo detach(NamespaceInfo namespace) {
        return detached(namespace, facade.detach(namespace));
    }

    public @Override NamespaceInfo getDefaultNamespace() {
        return facade.getDefaultNamespace();
    }

    public @Override void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        if (defaultNamespace != null) {
            NamespaceInfo ns = getNamespaceByPrefix(defaultNamespace.getPrefix());
            if (ns == null) {
                throw new IllegalArgumentException(
                        "No such namespace: '" + defaultNamespace.getPrefix() + "'");
            } else {
                defaultNamespace = ns;
            }
        }
        facade.setDefaultNamespace(defaultNamespace);
    }

    // Workspace methods
    public @Override void add(WorkspaceInfo workspace) {
        workspace = resolve(workspace);
        validate(workspace, true);

        if (getWorkspaceByName(workspace.getName()) != null) {
            throw new IllegalArgumentException(
                    "Workspace with name '" + workspace.getName() + "' already exists.");
        }

        WorkspaceInfo added;
        synchronized (facade) {
            beforeadded(workspace);
            added = facade.add(workspace);
            // if there is no default workspace use this one as the default
            if (getDefaultWorkspace() == null) {
                setDefaultWorkspace(workspace);
            }
        }

        added(added);
    }

    public @Override ValidationResult validate(WorkspaceInfo workspace, boolean isNew) {
        return validationSupport.validate(workspace, isNew);
    }

    @SuppressFBWarnings("NP_NULL_PARAM_DEREF") // I don't see this happening...
    public @Override void remove(WorkspaceInfo workspace) {
        validationSupport.beforeRemove(workspace);
        // TODO: remove synchronized block, need transactions
        synchronized (facade) {
            facade.remove(workspace);

            WorkspaceInfo defaultWorkspace = getDefaultWorkspace();
            if (workspace.equals(defaultWorkspace) || defaultWorkspace == null) {
                List<WorkspaceInfo> workspaces = facade.getWorkspaces();

                defaultWorkspace = null;
                if (!workspaces.isEmpty()) {
                    defaultWorkspace = workspaces.get(0);
                }

                setDefaultWorkspace(defaultWorkspace);
                if (defaultWorkspace != null) {
                    NamespaceInfo defaultNamespace =
                            getNamespaceByPrefix(defaultWorkspace.getName());
                    if (defaultNamespace != null) {
                        setDefaultNamespace(defaultNamespace);
                    }
                }
            }
        }

        removed(workspace);
    }

    public @Override void save(WorkspaceInfo workspace) {
        validate(workspace, false);

        facade.save(workspace);
    }

    public @Override WorkspaceInfo detach(WorkspaceInfo workspace) {
        return detached(workspace, facade.detach(workspace));
    }

    public @Override WorkspaceInfo getDefaultWorkspace() {
        return facade.getDefaultWorkspace();
    }

    public @Override void setDefaultWorkspace(WorkspaceInfo defaultWorkspace) {
        if (defaultWorkspace != null) {
            WorkspaceInfo ws = facade.getWorkspaceByName(defaultWorkspace.getName());
            if (ws == null) {
                throw new IllegalArgumentException(
                        "No such workspace: '" + defaultWorkspace.getName() + "'");
            } else {
                defaultWorkspace = ws;
            }
        }
        facade.setDefaultWorkspace(defaultWorkspace);
    }

    public @Override List<WorkspaceInfo> getWorkspaces() {
        return facade.getWorkspaces();
    }

    public @Override WorkspaceInfo getWorkspace(String id) {
        return facade.getWorkspace(id);
    }

    public @Override WorkspaceInfo getWorkspaceByName(String name) {
        if (name == null || Catalog.DEFAULT.equals(name)) {
            WorkspaceInfo ws = getDefaultWorkspace();
            if (ws != null) {
                name = ws.getName();
            }
        }
        return facade.getWorkspaceByName(name);
    }

    // Style methods
    public @Override StyleInfo getStyle(String id) {
        return facade.getStyle(id);
    }

    public @Override StyleInfo getStyleByName(String name) {
        StyleInfo result = null;
        int colon = name.indexOf(':');
        if (colon != -1) {
            // search by resource name
            String prefix = name.substring(0, colon);
            String resource = name.substring(colon + 1);

            result = getStyleByName(prefix, resource);
        } else {
            // search in default workspace first
            WorkspaceInfo ws = getDefaultWorkspace();
            if (ws != null) {
                result = getStyleByName(ws, name);
            }
        }
        if (result == null) {
            result = facade.getStyleByName(name);
        }

        return result;
    }

    public @Override StyleInfo getStyleByName(String workspaceName, String name) {
        if (workspaceName == null) {
            return getStyleByName((WorkspaceInfo) null, name);
        }

        WorkspaceInfo workspace = getWorkspaceByName(workspaceName);
        if (workspace != null) {
            return getStyleByName(workspace, name);
        }
        return null;
    }

    public @Override StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        if (workspace == null) {
            workspace = DefaultCatalogFacade.NO_WORKSPACE;
        }
        StyleInfo style = facade.getStyleByName(workspace, name);
        return style;
    }

    public @Override List<StyleInfo> getStyles() {
        return facade.getStyles();
    }

    public @Override List<StyleInfo> getStylesByWorkspace(String workspaceName) {
        WorkspaceInfo workspace = null;
        if (workspaceName != null) {
            workspace = getWorkspaceByName(workspaceName);
            if (workspace == null) {
                return Collections.emptyList();
            }
        }

        return getStylesByWorkspace(workspace);
    }

    public @Override List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return facade.getStylesByWorkspace(workspace);
    }

    public @Override void add(StyleInfo style) {
        style = resolve(style);
        validate(style, true);
        // set creation time before persisting
        beforeadded(style);
        StyleInfo added = facade.add(style);
        added(added);
    }

    public @Override ValidationResult validate(StyleInfo style, boolean isNew) {
        return validationSupport.validate(style, isNew);
    }

    public @Override void remove(StyleInfo style) {
        validationSupport.beforeRemove(style);
        facade.remove(style);
        removed(style);
    }

    public @Override void save(StyleInfo style) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(style);
        validate(style, false);

        // here we handle name changes
        int i = h.getPropertyNames().indexOf("name");
        if (i > -1 && !h.getNewValues().get(i).equals(h.getOldValues().get(i))) {
            String newName = (String) h.getNewValues().get(i);
            try {
                renameStyle(style, newName);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to rename style file along with name.", e);
            }
        }

        facade.save(style);
    }

    private void renameStyle(StyleInfo s, String newName) throws IOException {
        // rename style definition file
        Resource style = new GeoServerDataDirectory(resourceLoader).style(s);
        StyleHandler format = Styles.handler(s.getFormat());

        Resource target = Resources.uniqueResource(style, newName, format.getFileExtension());
        style.renameTo(target);
        s.setFilename(target.name());

        // rename generated sld if appropriate
        if (!SLDHandler.FORMAT.equals(format.getFormat())) {
            Resource sld = style.parent().get(FilenameUtils.getBaseName(style.name()) + ".sld");
            if (sld.getType() == Type.RESOURCE) {
                LOGGER.fine("Renaming style resource " + s.getName() + " to " + newName);

                Resource generated = Resources.uniqueResource(sld, newName, "sld");
                sld.renameTo(generated);
            }
        }
    }

    public @Override StyleInfo detach(StyleInfo style) {
        return detached(style, facade.detach(style));
    }

    // Event methods
    public @Override Collection<CatalogListener> getListeners() {
        return Collections.unmodifiableCollection(listeners);
    }

    public @Override void addListener(CatalogListener listener) {
        listeners.add(listener);
        @SuppressWarnings("unchecked")
        Comparator<CatalogListener> comparator = ExtensionPriority.COMPARATOR;
        Collections.sort(listeners, comparator);
    }

    public @Override void removeListener(CatalogListener listener) {
        listeners.remove(listener);
    }

    public @Override void removeListeners(@SuppressWarnings("rawtypes") Class listenerClass) {
        new ArrayList<>(listeners)
                .stream()
                .filter(l -> listenerClass.isInstance(l))
                .forEach(l -> listeners.remove(l));
    }

    public @Override ResourcePool getResourcePool() {
        return resourcePool;
    }

    public @Override void setResourcePool(ResourcePool resourcePool) {
        this.resourcePool = resourcePool;
    }

    public @Override GeoServerResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public @Override void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public @Override void dispose() {
        if (resourcePool != null) resourcePool.dispose();
        facade.dispose();
    }

    protected void added(CatalogInfo object) {
        fireAdded(object);
    }

    protected void beforeadded(CatalogInfo object) {
        fireBeforeAdded(object);
    }

    protected void removed(CatalogInfo object) {
        fireRemoved(object);
    }

    // @Override TODO: add to the interface
    public void fireBeforeAdded(CatalogInfo object) {
        CatalogBeforeAddEventImpl event = new CatalogBeforeAddEventImpl();
        event.setSource(object);
        event(event);
    }

    public @Override void fireAdded(CatalogInfo object) {
        CatalogAddEventImpl event = new CatalogAddEventImpl();
        event.setSource(object);

        event(event);
    }

    public @Override void fireModified(
            CatalogInfo object,
            List<String> propertyNames,
            @SuppressWarnings("rawtypes") List oldValues,
            @SuppressWarnings("rawtypes") List newValues) {
        CatalogModifyEventImpl event = new CatalogModifyEventImpl();

        event.setSource(object);
        event.setPropertyNames(propertyNames);
        event.setOldValues(oldValues);
        event.setNewValues(newValues);

        event(event);
    }

    public @Override void firePostModified(
            CatalogInfo object,
            List<String> propertyNames,
            @SuppressWarnings("rawtypes") List oldValues,
            @SuppressWarnings("rawtypes") List newValues) {
        CatalogPostModifyEventImpl event = new CatalogPostModifyEventImpl();
        event.setSource(object);
        event.setPropertyNames(propertyNames);
        event.setOldValues(oldValues);
        event.setNewValues(newValues);
        event(event);
    }

    public @Override void fireRemoved(CatalogInfo object) {
        CatalogRemoveEventImpl event = new CatalogRemoveEventImpl();
        event.setSource(object);

        event(event);
    }

    protected void event(CatalogEvent event) {
        CatalogException toThrow = null;

        for (Iterator<CatalogListener> l = listeners.iterator(); l.hasNext(); ) {
            try {
                CatalogListener listener = (CatalogListener) l.next();

                if (event instanceof CatalogAddEvent) {
                    listener.handleAddEvent((CatalogAddEvent) event);
                } else if (event instanceof CatalogRemoveEvent) {
                    listener.handleRemoveEvent((CatalogRemoveEvent) event);
                } else if (event instanceof CatalogModifyEvent) {
                    listener.handleModifyEvent((CatalogModifyEvent) event);
                } else if (event instanceof CatalogPostModifyEvent) {
                    listener.handlePostModifyEvent((CatalogPostModifyEvent) event);
                } else if (event instanceof CatalogBeforeAddEvent) {
                    listener.handlePreAddEvent((CatalogBeforeAddEvent) event);
                }
            } catch (Throwable t) {
                if (t instanceof CatalogException && toThrow == null) {
                    toThrow = (CatalogException) t;
                } else if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(
                            Level.WARNING, "Catalog listener threw exception handling event.", t);
                }
            }
        }

        if (toThrow != null) {
            throw toThrow;
        }
    }

    public static Object unwrap(Object obj) {
        return obj;
    }

    /** Implementation method for resolving all {@link ResolvingProxy} instances. */
    public void resolve() {
        facade.setCatalog(this);
        facade.resolve();

        if (listeners == null) {
            listeners = new ArrayList<CatalogListener>();
        }

        if (resourcePool == null) {
            resourcePool = ResourcePool.create(this);
        }
    }

    protected WorkspaceInfo resolve(WorkspaceInfo workspace) {
        resolveCollections(workspace);
        return workspace;
    }

    protected NamespaceInfo resolve(NamespaceInfo namespace) {
        resolveCollections(namespace);
        return namespace;
    }

    protected StoreInfo resolve(StoreInfo store) {
        resolveCollections(store);

        StoreInfoImpl s = (StoreInfoImpl) store;
        s.setCatalog(this);

        return store;
    }

    protected ResourceInfo resolve(ResourceInfo resource) {

        ResourceInfoImpl r = (ResourceInfoImpl) resource;
        r.setCatalog(this);

        if (resource instanceof FeatureTypeInfo) {
            resolve((FeatureTypeInfo) resource);
        }
        if (r instanceof CoverageInfo) {
            resolve((CoverageInfo) resource);
        }
        if (r instanceof WMSLayerInfo) {
            resolve((WMSLayerInfo) resource);
        }
        if (r instanceof WMTSLayerInfo) {
            resolve((WMTSLayerInfo) resource);
        }

        return resource;
    }

    private CoverageInfo resolve(CoverageInfo r) {
        CoverageInfoImpl c = (CoverageInfoImpl) r;
        if (c.getDimensions() != null) {
            for (CoverageDimensionInfo dim : c.getDimensions()) {
                if (dim.getNullValues() == null) {
                    ((CoverageDimensionImpl) dim).setNullValues(new ArrayList<Double>());
                }
            }
        }
        resolveCollections(r);
        return r;
    }

    /**
     * We don't want the world to be able and call this without going trough {@link
     * #resolve(ResourceInfo)}
     */
    private FeatureTypeInfo resolve(FeatureTypeInfo featureType) {
        FeatureTypeInfoImpl ft = (FeatureTypeInfoImpl) featureType;
        resolveCollections(ft);
        return ft;
    }

    private WMSLayerInfo resolve(WMSLayerInfo wmsLayer) {
        WMSLayerInfoImpl impl = (WMSLayerInfoImpl) wmsLayer;
        resolveCollections(impl);
        return wmsLayer;
    }

    private WMTSLayerInfo resolve(WMTSLayerInfo wmtsLayer) {
        WMTSLayerInfoImpl impl = (WMTSLayerInfoImpl) wmtsLayer;
        resolveCollections(impl);
        return wmtsLayer;
    }

    protected LayerInfo resolve(LayerInfo layer) {
        if (layer.getAttribution() == null) {
            layer.setAttribution(getFactory().createAttribution());
        }
        resolveCollections(layer);
        return layer;
    }

    protected LayerGroupInfo resolve(LayerGroupInfo layerGroup) {
        resolveCollections(layerGroup);
        return layerGroup;
    }

    protected StyleInfo resolve(StyleInfo style) {
        ((StyleInfoImpl) style).setCatalog(this);
        return style;
    }

    protected MapInfo resolve(MapInfo map) {
        resolveCollections(map);
        return map;
    }

    /** Method which reflectively sets all collections when they are null. */
    protected void resolveCollections(Object object) {
        OwsUtils.resolveCollections(object);
    }

    protected boolean isNull(String string) {
        return string == null || "".equals(string.trim());
    }

    <T extends CatalogInfo> T detached(T original, T detached) {
        return detached != null ? detached : original;
    }

    public void sync(CatalogImpl other) {
        other.facade.syncTo(facade);
        listeners = other.listeners;

        if (resourcePool != other.resourcePool) {
            resourcePool.dispose();
            resourcePool = other.resourcePool;
            resourcePool.setCatalog(this);
        }

        resourceLoader = other.resourceLoader;
    }

    public @Override void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    public void resolve(CatalogInfo info) {
        if (info instanceof LayerGroupInfo) {
            resolve((LayerGroupInfo) info);
        } else if (info instanceof LayerInfo) {
            resolve((LayerInfo) info);
        } else if (info instanceof MapInfo) {
            resolve((MapInfo) info);
        } else if (info instanceof NamespaceInfo) {
            resolve((NamespaceInfo) info);
        } else if (info instanceof ResourceInfo) {
            resolve((ResourceInfo) info);
        } else if (info instanceof StoreInfo) {
            resolve((StoreInfo) info);
        } else if (info instanceof StyleInfo) {
            resolve((StyleInfo) info);
        } else if (info instanceof WorkspaceInfo) {
            resolve((WorkspaceInfo) info);
        } else {
            throw new IllegalArgumentException("Unknown resource type: " + info);
        }
    }

    public @Override <T extends CatalogInfo> int count(final Class<T> of, final Filter filter) {
        final CatalogFacade facade = getFacade();
        return facade.count(of, filter);
    }

    public @Override <T extends CatalogInfo> CloseableIterator<T> list(
            final Class<T> of, final Filter filter) {
        return list(of, filter, null, null, null);
    }

    public @Override <T extends CatalogInfo> CloseableIterator<T> list(
            final Class<T> of,
            final Filter filter,
            Integer offset,
            Integer count,
            SortBy sortOrder) {
        CatalogFacade facade = getFacade();
        if (sortOrder != null
                && !facade.canSort(of, sortOrder.getPropertyName().getPropertyName())) {
            // TODO: use GeoTools' merge-sort code to provide sorting anyways
            throw new UnsupportedOperationException(
                    "Catalog backend can't sort on property "
                            + sortOrder.getPropertyName()
                            + " in-process sorting is pending implementation");
        }
        if (sortOrder != null) {
            return facade.list(of, filter, offset, count, sortOrder);
        } else {
            return facade.list(of, filter, offset, count);
        }
    }

    public @Override <T extends CatalogInfo> T get(Class<T> type, Filter filter)
            throws IllegalArgumentException {

        final Integer limit = Integer.valueOf(2);
        CloseableIterator<T> it = list(type, filter, null, limit, null);
        T result = null;
        try {
            if (it.hasNext()) {
                result = it.next();
                if (it.hasNext()) {
                    throw new IllegalArgumentException(
                            "Specified query predicate resulted in more than one object");
                }
            }
        } finally {
            it.close();
        }
        return result;
    }

    public @Override CatalogCapabilities getCatalogCapabilities() {
        return facade.getCatalogCapabilities();
    }
}
