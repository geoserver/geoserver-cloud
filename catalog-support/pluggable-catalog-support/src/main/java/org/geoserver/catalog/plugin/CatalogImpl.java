/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static java.util.Collections.unmodifiableList;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogCapabilities;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.ValidationResult;
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
import org.geoserver.catalog.impl.DefaultCatalogFacade;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ProxyUtils;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.resolving.ModificationProxyDecorator;
import org.geoserver.catalog.plugin.resolving.ResolvingCatalogFacade;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Alternative to {@link org.geoserver.catalog.impl.CatalogImpl} to improve separation of concerns
 * between levels of abstractions and favor plug-ability of the underlying object store.
 *
 * <p>
 *
 * <ul>
 *   <li>Allows decorating the {@link CatalogFacade} with an {@link IsolatedCatalogFacade} when
 *       {@link #setFacade} is called, instead of only in the default constructor
 *   <li>Requires the {@code CatalogFacade} to derive from {@link ExtendedCatalogFacade}, to make
 *       use of {@link ExtendedCatalogFacade#query query(Query&lt;T&gt;):Stream&lt;T&gt;} and {@link
 *       ExtendedCatalogFacade#update update(CatalogInfo, Patch)}
 *   <li>Enables setting a {@link RepositoryCatalogFacade}, which allows to easily abstract out the
 *       underlying backend storage using {@link CatalogInfoRepository} implemenatations
 *   <li>Uses {@link DefaultMemoryCatalogFacade} as the default facade implementation for attached,
 *       on-heap {@link CatalogInfo} storage
 *   <li>Implements all business-logic, like event handling and ensuring no {@link CatalogInfo}
 *       instance gets in or out of the {@link Catalog} without being decorated with a {@link
 *       ModificationProxy}, relieving the lower-level {@link CatalogFacade} abstraction of such
 *       concerns. Hence {@link ExtendedCatalogFacade} works on plain POJOS, or whatever is supplied
 *       by its {@link CatalogInfoRepository repositories}, though in practice it can only be
 *       implementations of {@code org.geoserver.catalog.impl.*InfoImpl} due to coupling in other
 *       areas.
 *   <li>Of special interest is the use of {@link PropertyDiff} and {@link Patch} on all the {@link
 *       #save} methods, delegating to {@link ExtendedCatalogFacade#update(CatalogInfo, Patch)} , in
 *       order to keep the {@code ModificationProxy} logic local to this catalog implementation, and
 *       let the backend (facade) implement atomic updates as it fits it better.
 * </ul>
 */
@SuppressWarnings("serial")
public class CatalogImpl implements Catalog {

    /** logger */
    private static final Logger LOGGER = Logging.getLogger(CatalogImpl.class);

    /**
     * Original data access facade provided at {@link #setFacade(CatalogFacade)}, may or may be not
     * a {@link ResolvingCatalogFacade}. If not, {@link #facade} will be a resolving decorator to
     * allow traits to be added.
     */
    protected CatalogFacade rawFacade;

    /**
     * Resolving catalog facade to use inside this catalog. The {@link #rawFacade} will be wrapped
     * on a resolving decorator if it's not already a {@link ResolvingCatalogFacade}.
     *
     * <p>This catalog will add inbound and outbound traits to make sure no {@link CatalogInfo}
     * leaves the facade without being decorated with a {@link ModificationProxy}, nor gets into the
     * facade without its {@link ModificationProxy} decorator being removed.
     */
    protected ResolvingCatalogFacade facade;

    /** listeners */
    protected List<CatalogListener> listeners = new CopyOnWriteArrayList<>();

    /** resources */
    protected ResourcePool resourcePool;

    protected GeoServerResourceLoader resourceLoader;

    /** Handles {@link CatalogInfo} validation rules before adding or updating an object */
    protected final CatalogValidationRules validationSupport;

    protected final boolean isolated;

    public CatalogImpl() {
        this(true);
    }

    public CatalogImpl(boolean isolated) {
        this(new DefaultMemoryCatalogFacade(), isolated);
    }

    public CatalogImpl(CatalogFacade facade) {
        this(facade, true);
    }

    public CatalogImpl(CatalogFacade facade, boolean isolated) {
        Objects.requireNonNull(facade);
        this.isolated = isolated;
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
        // final GeoServerConfigurationLock configurationLock;
        // configurationLock = GeoServerExtensions.bean(GeoServerConfigurationLock.class);
        // if (configurationLock != null) {
        // facade = LockingCatalogFacade.create(facade, configurationLock);
        // }
        ExtendedCatalogFacade efacade;
        Function<CatalogInfo, CatalogInfo> outboundResolver = Function.identity();
        if (facade instanceof ExtendedCatalogFacade) {
            efacade = (ExtendedCatalogFacade) facade;
        } else {
            efacade = new CatalogFacadeExtensionAdapter(facade);
            outboundResolver = ModificationProxyDecorator.unwrap();
        }
        // decorate the default catalog facade with one capable of handling isolated workspaces
        // behavior
        if (this.isolated) {
            efacade = new IsolatedCatalogFacade(efacade);
        }
        ResolvingCatalogFacade resolving = new ResolvingCatalogFacade(efacade);
        // make sure no object leaves without being proxies, nor enters the facade as a proxy. Note
        // it is ok if the provided facade is already a ResolvingCatalogFacade. This catalog doesn't
        // care which object resolution chain the provided facade needs to perform.
        resolving.setOutboundResolver(outboundResolver.andThen(ModificationProxyDecorator.wrap()));
        resolving.setInboundResolver(ModificationProxyDecorator::unwrap);
        this.facade = resolving;
        this.facade.setCatalog(this);
    }

    public @Override String getId() {
        return "catalog";
    }

    public @Override CatalogFactory getFactory() {
        return new CatalogFactoryImpl(this);
    }

    // Store methods
    public @Override void add(StoreInfo store) {
        validate(store, true);

        // TODO: remove synchronized block, need transactions
        StoreInfo added;
        synchronized (facade) {
            beforeadded(store);
            added = facade.add(store);

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

    /**
     * This is not API but we need to decide if MapInfo is deprecated/removed or further developed
     */
    public ValidationResult validate(MapInfo map, boolean isNew) {
        return validationSupport.validate(map, isNew);
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
        doSave(store);
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

        return unmodifiableList(facade.getStoresByWorkspace(workspace, clazz));
    }

    public @Override <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return unmodifiableList(facade.getStores(clazz));
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
        validate(resource, true);
        beforeadded(resource);
        ResourceInfo added = facade.add(resource);
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
        doSave(resource);
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
        return unmodifiableList(facade.getResources(clazz));
    }

    public @Override <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        return unmodifiableList(facade.getResourcesByNamespace(namespace, clazz));
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
        return unmodifiableList(facade.getResourcesByStore(store, clazz));
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
        validate(layer, true);
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
        doSave(layer);
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
        return unmodifiableList(facade.getLayers(resource));
    }

    public @Override List<LayerInfo> getLayers(StyleInfo style) {
        return unmodifiableList(facade.getLayers(style));
    }

    public @Override List<LayerInfo> getLayers() {
        return unmodifiableList(facade.getLayers());
    }

    // Map methods
    public @Override MapInfo getMap(String id) {
        return facade.getMap(id);
    }

    public @Override MapInfo getMapByName(String name) {
        return facade.getMapByName(name);
    }

    public @Override List<MapInfo> getMaps() {
        return unmodifiableList(facade.getMaps());
    }

    public @Override void add(LayerGroupInfo layerGroup) {
        validate(layerGroup, true);
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
        doSave(layerGroup);
    }

    public @Override LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return detached(layerGroup, facade.detach(layerGroup));
    }

    public @Override List<LayerGroupInfo> getLayerGroups() {
        return unmodifiableList(facade.getLayerGroups());
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
        return unmodifiableList(facade.getLayerGroupsByWorkspace(workspace));
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
        validate(map, true);
        beforeadded(map);
        MapInfo added = facade.add(map);
        added(added);
    }

    public @Override void remove(MapInfo map) {
        validationSupport.beforeRemove(map);
        facade.remove(map);
        removed(map);
    }

    public @Override void save(MapInfo map) {
        validate(map, false);
        doSave(map);
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
        return unmodifiableList(facade.getNamespaces());
    }

    public @Override void add(NamespaceInfo namespace) {
        validate(namespace, true);

        NamespaceInfo added;
        synchronized (facade) {
            beforeadded(namespace);
            added = facade.add(namespace);
            if (getDefaultNamespace() == null) {
                setDefaultNamespace(added);
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
        doSave(namespace);
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
        validate(workspace, true);

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
        doSave(workspace);
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
        return unmodifiableList(facade.getWorkspaces());
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
        return unmodifiableList(facade.getStyles());
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
        return unmodifiableList(facade.getStylesByWorkspace(workspace));
    }

    public @Override void add(StyleInfo style) {
        validate(style, true);
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
        validate(style, false);

        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(style);
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

        doSave(style);
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

    /**
     * Implementation method for resolving all {@link ResolvingProxy} instances.
     *
     * <p>(GR)REVISIT: what do ResolvingProxy instances have to do with a default catalog
     * implementation? this is not even API. It's up to the calling code to resolve objects and
     * provide valid input. The {@link ResolvingCatalogFacade} can be of good use for the default
     * geoserver loader here, while it should load in order to guarantee presence of dependent
     * objects.
     */
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

    <T extends CatalogInfo> T detached(T original, T detached) {
        return detached != null ? detached : original;
    }

    public void sync(Catalog other) {
        other.getFacade().syncTo(facade);
        listeners.clear();
        listeners.addAll(other.getListeners());

        ResourcePool resourcePool = other.getResourcePool();
        // REVISIT: this still sounds wrong... looks like an old assumption that both catalogs are
        // in-memory catalogs and the argument one is to be disposed, which in the end means this
        // method
        // does not belong here but to whom's calling (DefaultGeoServerLoader?)
        if (this.resourcePool != resourcePool) {
            this.resourcePool.dispose();
        }
        setResourcePool(resourcePool);
        resourcePool.setCatalog(this);
        resourceLoader = other.getResourceLoader();
    }

    public @Override void accept(CatalogVisitor visitor) {
        visitor.visit(this);
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
        T result = null;
        try (CloseableIterator<T> it = list(type, filter, null, limit, null)) {
            if (it.hasNext()) {
                result = it.next();
                if (it.hasNext()) {
                    throw new IllegalArgumentException(
                            "Specified query predicate resulted in more than one object");
                }
            }
        }
        return result;
    }

    public @Override CatalogCapabilities getCatalogCapabilities() {
        return facade.getCatalogCapabilities();
    }

    /**
     * Called by all {@code save(...)} methods, creates a {@link Patch} out of the {@code info}
     * {@link ModificationProxy} and calls {@link ExtendedCatalogFacade#update
     * facade.update(CatalogInfo, Patch)} with the real object and the patch, for the facade to
     * apply the changeset to its backend storage as appropriate.
     *
     * <p>Handles {@link #fireModified pre} and {@link #firePostModified post} modify events
     * publishing. It is no longer {@link CatalogFacade}s responsibility to publish the post-modify
     * events
     *
     * @param info a {@link ModificationProxy} holding the actual, unchanged object and the changed
     *     properties
     */
    protected <I extends CatalogInfo> void doSave(final I info) {
        ModificationProxy proxy = ProxyUtils.handler(info, ModificationProxy.class);
        // figure out what changed
        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> newValues = proxy.getNewValues();
        List<Object> oldValues = proxy.getOldValues();

        // this could be the event's payload instead of three separate lists
        PropertyDiff diff = PropertyDiff.valueOf(proxy);
        Patch patch = diff.toPatch();

        // use the proxied object, may some listener change it
        fireModified(info, propertyNames, oldValues, newValues);

        // note info will be unwrapped before being given to the raw facade by the inbound resolving
        // function set at #setFacade
        I updated = facade.update(info, patch);

        // commit proxy, making effective the change in the provided object. Has no effect in what's
        // been passed to the facade
        proxy.commit();

        firePostModified(updated, propertyNames, oldValues, newValues);
    }
}
