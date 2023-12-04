/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static java.util.Collections.unmodifiableList;

import com.google.common.annotations.VisibleForTesting;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogCapabilities;
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
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.impl.CatalogAddEventImpl;
import org.geoserver.catalog.event.impl.CatalogBeforeAddEventImpl;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogPostModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogRemoveEventImpl;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.DefaultCatalogFacade;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ProxyUtils;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.catalog.plugin.resolving.ModificationProxyDecorator;
import org.geoserver.catalog.plugin.rules.CatalogBusinessRules;
import org.geoserver.catalog.plugin.rules.CatalogOpContext;
import org.geoserver.catalog.plugin.validation.CatalogValidationRules;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.identity.Identifier;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.Converters;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.annotation.Nullable;

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
 *       underlying backend storage using {@link CatalogInfoRepository} implementations
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
 *
 * <p>NOTE: subclass of {@link CatalogImpl} due to several unchecked casts in {@link
 * GeoServerLoader}, {@link GeoServerImpl}, {@link XStreamPersister}, {@link DefaultCatalogFacade}
 * and others. We should really code to the interface and leave non API code out of CatalogImpl and
 * into helper classes!
 */
@SuppressWarnings({"serial", "rawtypes", "unchecked"}) // REMOVE once we can stop inheriting from
// CatalogImpl
public class CatalogPlugin extends CatalogImpl implements Catalog {

    /** logger */
    private static final Logger LOGGER = Logging.getLogger(CatalogPlugin.class);

    /**
     * Original data access facade provided at {@link #setFacade(CatalogFacade)}, may or may be not
     * a {@link ResolvingCatalogFacadeDecorator}. If not, {@link #facade} will be a resolving
     * decorator to allow traits to be added.
     */
    protected CatalogFacade rawFacade;

    /** Handles {@link CatalogInfo} validation rules before adding or updating an object */
    protected final CatalogValidationRules validationSupport;

    private final CatalogBusinessRules businessRules = new CatalogBusinessRules();

    protected final boolean isolated;

    public CatalogPlugin() {
        this(true);
    }

    public CatalogPlugin(boolean isolated) {
        this(new DefaultMemoryCatalogFacade(), isolated);
    }

    public CatalogPlugin(CatalogFacade facade) {
        this(facade, true);
    }

    public CatalogPlugin(CatalogFacade facade, boolean isolated) {
        Objects.requireNonNull(facade);
        this.isolated = isolated;
        setFacade(facade);
        resourcePool = ResourcePool.create(this);
        validationSupport = new CatalogValidationRules(this);
    }

    /** Constructor for {@link #getRawCatalog()} */
    private CatalogPlugin(CatalogPlugin catalog) {
        super(catalog); // sets dispatcher and resourcePool
        this.isolated = false;
        super.resourcePool = catalog.resourcePool;
        super.resourceLoader = catalog.resourceLoader;
        validationSupport = new CatalogValidationRules(this);
        // use setFacade to wrap it in a modificationproxy resolver if needed
        // sets both rawFacade and facade
        setFacade(catalog.getRawFacade());
    }

    /**
     * Returns a truly raw version of the CatalogImpl, that means with a raw catalog facade instead
     * of the Isolated Workspace one, nothing is filtered or hidden. Only for usage by the
     * ResolvingProxy, should otherwise never be used.
     */
    @Override
    public CatalogPlugin getRawCatalog() {
        return new CatalogPlugin(this);
    }

    @Override
    public ExtendedCatalogFacade getFacade() {
        return (ExtendedCatalogFacade) facade;
    }

    public CatalogFacade getRawFacade() {
        return rawFacade;
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
        this.rawFacade = facade;
        ExtendedCatalogFacade efacade;
        Function<CatalogInfo, CatalogInfo> outboundResolver;
        Function<CatalogInfo, CatalogInfo> inboundResolver;
        if (facade instanceof ExtendedCatalogFacade extended) {
            efacade = extended;
            // make sure no object leaves the catalog without being proxied, nor enters the facade
            // as a proxy. Note it is ok if the provided facade is already a ResolvingCatalogFacade.
            // This catalog doesn't care which object resolution chain the provided facade needs to
            // perform.
            outboundResolver = ModificationProxyDecorator::wrap;
            inboundResolver = ModificationProxyDecorator::unwrap;
        } else {
            efacade = new CatalogFacadeExtensionAdapter(facade);
            outboundResolver = Function.identity();
            inboundResolver = Function.identity();
        }
        // decorate the default catalog facade with one capable of handling isolated workspaces
        if (this.isolated) {
            efacade = new IsolatedCatalogFacade(efacade);
        }
        ResolvingCatalogFacadeDecorator resolving = new ResolvingCatalogFacadeDecorator(efacade);
        resolving.setOutboundResolver(outboundResolver);
        resolving.setInboundResolver(inboundResolver);
        this.facade = resolving;
        this.facade.setCatalog(this);
    }

    @Override
    public String getId() {
        return "catalog";
    }

    @Override
    public CatalogFactory getFactory() {
        return new CatalogFactoryImpl(this);
    }

    // Store methods
    @Override
    public void add(StoreInfo store) {
        if (store.getWorkspace() == null) {
            store.setWorkspace(getDefaultWorkspace());
        }
        doAdd(store, facade::add);
    }

    @Override
    public ValidationResult validate(StoreInfo store, boolean isNew) {
        return validationSupport.validate(store, isNew);
    }

    /**
     * This is not API but we need to decide if MapInfo is deprecated/removed or further developed
     */
    public ValidationResult validate(MapInfo map, boolean isNew) {
        return validationSupport.validate(map, isNew);
    }

    @Override
    public void remove(StoreInfo store) {
        doRemove(store, facade::remove);
    }

    // TODO: move the namespace update logic to validationrules.onBefore/AfterSave and just call
    // doSave(store)
    @Override
    public void save(StoreInfo store) {
        if (store.getId() == null) {
            // some code uses save() when it should use add()
            add(store);
            return;
        }

        final StoreInfo oldState = getStore(store.getId(), StoreInfo.class);
        final WorkspaceInfo oldWorkspace = oldState.getWorkspace();

        // figure out if the store's workspace changed before saving it and get the namespace to
        // update its ResourceInfos
        final Optional<NamespaceInfo> namespaceChange = findNamespaceChange(store, oldWorkspace);
        // save a copy of the store's state before saving in case it has to be rolled back
        final StoreInfo rollbackCopy =
                namespaceChange.map(ns -> cloneForRollback(oldState)).orElse(null);

        // save the store before updating the resources namespace, don't risk a listener getting an
        // inconsistent state while saving a ResourceInfo if the workspace changed
        doSave(store);

        // if updateResourcesNamespace returns, all its resources have been updated, if it fails
        // they've been rolled back
        try {
            namespaceChange.ifPresent(
                    newNamespace ->
                            updateResourcesNamespace(store, oldWorkspace.getName(), newNamespace));
        } catch (RuntimeException e) {
            rollback(store, rollbackCopy);
            throw e;
        }
    }

    protected @Override void updateResourcesNamespace(
            StoreInfo store, String oldNamespacePrefix, NamespaceInfo newNamespace) {
        List<ResourceInfo> storeResources = getResourcesByStore(store, ResourceInfo.class);
        try {
            storeResources.stream().forEach(resource -> updateNamespace(resource, newNamespace));
        } catch (RuntimeException e) {
            rollbackNamespaces(storeResources, oldNamespacePrefix, newNamespace);
            throw e;
        }
    }

    protected @Override void rollbackNamespaces(
            List<ResourceInfo> storeResources,
            String oldNamespacePrefix,
            NamespaceInfo newNamespace) {
        // rolback the namespace on all the resources that got it changed
        final NamespaceInfo rolbackNamespace = getNamespaceByPrefix(oldNamespacePrefix);
        Objects.requireNonNull(rolbackNamespace);

        final String newNsId = newNamespace.getId();
        storeResources.stream()
                .filter(r -> newNsId.equals(r.getNamespace().getId()))
                .forEach(
                        resource -> {
                            updateNamespace(resource, rolbackNamespace);
                        });
    }

    // override, super calls facade.save and depends on it throwing the events
    protected @Override void rollback(StoreInfo store, StoreInfo rollbackTo) {
        // apply the rollback object properties to the real store
        OwsUtils.copy(rollbackTo, store, infoInterfaceOf(rollbackTo));
        doSave(store);
    }

    // override, super calls facade.save and depends on it throwing the events
    @VisibleForTesting
    @Override
    public void updateNamespace(ResourceInfo resource, NamespaceInfo newNamespace) {
        resource.setNamespace(newNamespace);
        doSave(resource);
    }

    @Override
    public <T extends StoreInfo> T detach(T store) {
        return detach(store, facade.detach(store));
    }

    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return facade.getStore(id, clazz);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Override
    public <T extends StoreInfo> T getStoreByName(String name, Class<T> clazz) {
        return getStoreByName((WorkspaceInfo) null, name, clazz);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Override
    public <T extends StoreInfo> T getStoreByName(
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

    @Override
    public <T extends StoreInfo> T getStoreByName(
            String workspaceName, String name, Class<T> clazz) {

        WorkspaceInfo workspace = getWorkspaceByName(workspaceName);
        if (workspace != null) {
            return getStoreByName(workspace, name, clazz);
        }
        return null;
    }

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(
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

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {

        return unmodifiableList(facade.getStoresByWorkspace(workspace, clazz));
    }

    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return unmodifiableList(facade.getStores(clazz));
    }

    @Override
    public WMSStoreInfo getWMSStore(String id) {
        return getStore(id, WMSStoreInfo.class);
    }

    @Override
    public WMSStoreInfo getWMSStoreByName(String name) {
        return getStoreByName(name, WMSStoreInfo.class);
    }

    @Override
    public WMTSStoreInfo getWMTSStore(String id) {
        return getStore(id, WMTSStoreInfo.class);
    }

    @Override
    public WMTSStoreInfo getWMTSStoreByName(String name) {
        return getStoreByName(name, WMTSStoreInfo.class);
    }

    @Override
    public DataStoreInfo getDataStore(String id) {
        return (DataStoreInfo) getStore(id, DataStoreInfo.class);
    }

    @Override
    public DataStoreInfo getDataStoreByName(String name) {
        return (DataStoreInfo) getStoreByName(name, DataStoreInfo.class);
    }

    @Override
    public DataStoreInfo getDataStoreByName(String workspaceName, String name) {
        return (DataStoreInfo) getStoreByName(workspaceName, name, DataStoreInfo.class);
    }

    @Override
    public DataStoreInfo getDataStoreByName(WorkspaceInfo workspace, String name) {
        return (DataStoreInfo) getStoreByName(workspace, name, DataStoreInfo.class);
    }

    @Override
    public List<DataStoreInfo> getDataStoresByWorkspace(String workspaceName) {
        return getStoresByWorkspace(workspaceName, DataStoreInfo.class);
    }

    @Override
    public List<DataStoreInfo> getDataStoresByWorkspace(WorkspaceInfo workspace) {
        return getStoresByWorkspace(workspace, DataStoreInfo.class);
    }

    @Override
    public List<DataStoreInfo> getDataStores() {
        return getStores(DataStoreInfo.class);
    }

    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return facade.getDefaultDataStore(workspace);
    }

    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
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

        DataStoreInfo old = getDefaultDataStore(workspace);
        // fire modify event before change
        fireModified(this, asList("defaultDataStore"), asList(old), asList(store));

        facade.setDefaultDataStore(workspace, store);

        // fire postmodify event after change
        firePostModified(this, asList("defaultDataStore"), asList(old), asList(store));
    }

    @Override
    public CoverageStoreInfo getCoverageStore(String id) {
        return (CoverageStoreInfo) getStore(id, CoverageStoreInfo.class);
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(String name) {
        return (CoverageStoreInfo) getStoreByName(name, CoverageStoreInfo.class);
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(String workspaceName, String name) {
        return getStoreByName(workspaceName, name, CoverageStoreInfo.class);
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(WorkspaceInfo workspace, String name) {
        return getStoreByName(workspace, name, CoverageStoreInfo.class);
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(String workspaceName) {
        return getStoresByWorkspace(workspaceName, CoverageStoreInfo.class);
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(WorkspaceInfo workspace) {
        return getStoresByWorkspace(workspace, CoverageStoreInfo.class);
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStores() {
        return getStores(CoverageStoreInfo.class);
    }

    // Resource methods
    @Override
    public void add(ResourceInfo resource) {
        if (resource.getNamespace() == null) {
            // default to default namespace
            resource.setNamespace(getDefaultNamespace());
        }
        if (resource.getNativeName() == null) {
            resource.setNativeName(resource.getName());
        }
        doAdd(resource, facade::add);
    }

    @Override
    public ValidationResult validate(ResourceInfo resource, boolean isNew) {
        return validationSupport.validate(resource, isNew);
    }

    @Override
    public void remove(ResourceInfo resource) {
        doRemove(resource, facade::remove);
    }

    @Override
    public void save(ResourceInfo resource) {
        doSave(resource);
    }

    @Override
    public <T extends ResourceInfo> T detach(T resource) {
        return detach(resource, facade.detach(resource));
    }

    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return facade.getResource(id, clazz);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Override
    public <T extends ResourceInfo> T getResourceByName(String ns, String name, Class<T> clazz) {
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
    @Override
    public <T extends ResourceInfo> T getResourceByName(
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

    @Override
    public <T extends ResourceInfo> T getResourceByName(Name name, Class<T> clazz) {
        return getResourceByName(name.getNamespaceURI(), name.getLocalPart(), clazz);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Override
    public <T extends ResourceInfo> T getResourceByName(String name, Class<T> clazz) {
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

    @Override
    public /* <T extends ResourceInfo> List<T> */ List getResources(Class /* <T> */ clazz) {
        return unmodifiableList(facade.getResources(clazz));
    }

    @Override
    public /* <T extends ResourceInfo> List<T> */ List getResourcesByNamespace(
            NamespaceInfo namespace, Class /* <T> */ clazz) {
        return unmodifiableList(facade.getResourcesByNamespace(namespace, clazz));
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
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

    @Override
    public <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        return facade.getResourceByStore(store, name, clazz);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return unmodifiableList(facade.getResourcesByStore(store, clazz));
    }

    @Override
    public FeatureTypeInfo getFeatureType(String id) {
        return (FeatureTypeInfo) getResource(id, FeatureTypeInfo.class);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(String ns, String name) {
        return (FeatureTypeInfo) getResourceByName(ns, name, FeatureTypeInfo.class);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(NamespaceInfo ns, String name) {
        return getResourceByName(ns, name, FeatureTypeInfo.class);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(Name name) {
        return getResourceByName(name, FeatureTypeInfo.class);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(String name) {
        return (FeatureTypeInfo) getResourceByName(name, FeatureTypeInfo.class);
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypes() {
        return getResources(FeatureTypeInfo.class);
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypesByNamespace(NamespaceInfo namespace) {
        return getResourcesByNamespace(namespace, FeatureTypeInfo.class);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByDataStore(DataStoreInfo dataStore, String name) {
        return getResourceByStore(dataStore, name, FeatureTypeInfo.class);
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypesByDataStore(DataStoreInfo store) {
        return getResourcesByStore(store, FeatureTypeInfo.class);
    }

    @Override
    public CoverageInfo getCoverage(String id) {
        return (CoverageInfo) getResource(id, CoverageInfo.class);
    }

    @Override
    public CoverageInfo getCoverageByName(String ns, String name) {
        return (CoverageInfo) getResourceByName(ns, name, CoverageInfo.class);
    }

    @Override
    public CoverageInfo getCoverageByName(NamespaceInfo ns, String name) {
        return (CoverageInfo) getResourceByName(ns, name, CoverageInfo.class);
    }

    @Override
    public CoverageInfo getCoverageByName(Name name) {
        return getResourceByName(name, CoverageInfo.class);
    }

    @Override
    public CoverageInfo getCoverageByName(String name) {
        return (CoverageInfo) getResourceByName(name, CoverageInfo.class);
    }

    @Override
    public List<CoverageInfo> getCoverages() {
        return getResources(CoverageInfo.class);
    }

    @Override
    public List<CoverageInfo> getCoveragesByNamespace(NamespaceInfo namespace) {
        return getResourcesByNamespace(namespace, CoverageInfo.class);
    }

    @Override
    public List<CoverageInfo> getCoveragesByStore(CoverageStoreInfo store) {
        return getResourcesByStore(store, CoverageInfo.class);
    }

    @Override
    public CoverageInfo getCoverageByCoverageStore(CoverageStoreInfo coverageStore, String name) {
        return getResourceByStore(coverageStore, name, CoverageInfo.class);
    }

    @Override
    public List<CoverageInfo> getCoveragesByCoverageStore(CoverageStoreInfo store) {
        return getResourcesByStore(store, CoverageInfo.class);
    }

    // Layer methods
    @Override
    public void add(LayerInfo layer) {
        doAdd(layer, facade::add);
    }

    @Override
    public ValidationResult validate(LayerInfo layer, boolean isNew) {
        return validationSupport.validate(layer, isNew);
    }

    @Override
    public void remove(LayerInfo layer) {
        doRemove(layer, facade::remove);
    }

    @Override
    public void save(LayerInfo layer) {
        doSave(layer);
    }

    @Override
    public LayerInfo detach(LayerInfo layer) {
        return detach(layer, facade.detach(layer));
    }

    @Override
    public LayerInfo getLayer(String id) {
        return facade.getLayer(id);
    }

    @Override
    public LayerInfo getLayerByName(Name name) {
        if (name.getNamespaceURI() != null) {
            NamespaceInfo ns = getNamespaceByURI(name.getNamespaceURI());
            if (ns != null) {
                return getLayerByName(ns.getPrefix() + ":" + name.getLocalPart());
            }
        }

        return getLayerByName(name.getLocalPart());
    }

    @Override
    public LayerInfo getLayerByName(String name) {
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

    public static LayerInfo getLayerByName(Catalog catalog, String workspace, String resourceName) {
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

    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return unmodifiableList(facade.getLayers(resource));
    }

    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {
        return unmodifiableList(facade.getLayers(style));
    }

    @Override
    public List<LayerInfo> getLayers() {
        return unmodifiableList(facade.getLayers());
    }

    // Map methods
    @Override
    public MapInfo getMap(String id) {
        return facade.getMap(id);
    }

    @Override
    public MapInfo getMapByName(String name) {
        return facade.getMapByName(name);
    }

    @Override
    public List<MapInfo> getMaps() {
        return unmodifiableList(facade.getMaps());
    }

    @Override
    public void add(LayerGroupInfo layerGroup) {
        doAdd(layerGroup, facade::add);
    }

    @Override
    public ValidationResult validate(LayerGroupInfo layerGroup, boolean isNew) {
        return validationSupport.validate(layerGroup, isNew);
    }

    @Override
    public void remove(LayerGroupInfo layerGroup) {
        doRemove(layerGroup, facade::remove);
    }

    @Override
    public void save(LayerGroupInfo layerGroup) {
        doSave(layerGroup);
    }

    @Override
    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return detach(layerGroup, facade.detach(layerGroup));
    }

    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return unmodifiableList(facade.getLayerGroups());
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName) {
        WorkspaceInfo workspace = null;
        if (workspaceName != null) {
            workspace = getWorkspaceByName(workspaceName);
            if (workspace == null) {
                return Collections.emptyList();
            }
        }

        return getLayerGroupsByWorkspace(workspace);
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return unmodifiableList(facade.getLayerGroupsByWorkspace(workspace));
    }

    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return facade.getLayerGroup(id);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {

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

    @Override
    public LayerGroupInfo getLayerGroupByName(String workspaceName, String name) {
        WorkspaceInfo workspace = null;
        if (workspaceName != null) {
            workspace = getWorkspaceByName(workspaceName);
            if (workspace == null) {
                return null;
            }
        }

        return getLayerGroupByName(workspace, name);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {

        if (null == workspace) {
            workspace = DefaultCatalogFacade.NO_WORKSPACE;
        }

        return facade.getLayerGroupByName(workspace, name);
    }

    @Override
    public void add(MapInfo map) {
        doAdd(map, facade::add);
    }

    @Override
    public void remove(MapInfo map) {
        doRemove(map, facade::remove);
    }

    @Override
    public void save(MapInfo map) {
        doSave(map);
    }

    @Override
    public MapInfo detach(MapInfo map) {
        return detach(map, facade.detach(map));
    }

    // Namespace methods
    @Override
    public NamespaceInfo getNamespace(String id) {
        return facade.getNamespace(id);
    }

    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        if (prefix == null || Catalog.DEFAULT.equals(prefix)) {
            NamespaceInfo ns = getDefaultNamespace();
            if (ns != null) {
                prefix = ns.getPrefix();
            }
        }

        return facade.getNamespaceByPrefix(prefix);
    }

    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        return facade.getNamespaceByURI(uri);
    }

    @Override
    public List<NamespaceInfo> getNamespaces() {
        return unmodifiableList(facade.getNamespaces());
    }

    @Override
    public void add(NamespaceInfo namespace) {
        doAdd(namespace, facade::add);
    }

    @Override
    public ValidationResult validate(NamespaceInfo namespace, boolean isNew) {
        return validationSupport.validate(namespace, isNew);
    }

    @Override
    public void remove(NamespaceInfo namespace) {
        doRemove(namespace, facade::remove);
    }

    @Override
    public void save(NamespaceInfo namespace) {
        doSave(namespace);
    }

    @Override
    public NamespaceInfo detach(NamespaceInfo namespace) {
        return detach(namespace, facade.detach(namespace));
    }

    @Override
    public NamespaceInfo getDefaultNamespace() {
        return facade.getDefaultNamespace();
    }

    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        if (defaultNamespace != null) {
            NamespaceInfo ns = getNamespaceByPrefix(defaultNamespace.getPrefix());
            if (ns == null) {
                throw new IllegalArgumentException(
                        "No such namespace: '" + defaultNamespace.getPrefix() + "'");
            } else {
                defaultNamespace = ns;
            }
        }

        NamespaceInfo old = getDefaultNamespace();
        // fire modify event before change
        fireModified(this, asList("defaultNamespace"), asList(old), asList(defaultNamespace));

        facade.setDefaultNamespace(defaultNamespace);

        // fire postmodify event after change
        firePostModified(this, asList("defaultNamespace"), asList(old), asList(defaultNamespace));
    }

    // Workspace methods
    @Override
    public void add(WorkspaceInfo workspace) {
        doAdd(workspace, facade::add);
    }

    @Override
    public ValidationResult validate(WorkspaceInfo workspace, boolean isNew) {
        return validationSupport.validate(workspace, isNew);
    }

    @Override
    public void remove(WorkspaceInfo workspace) {
        doRemove(workspace, facade::remove);
    }

    @Override
    public void save(WorkspaceInfo workspace) {
        doSave(workspace);
    }

    @Override
    public WorkspaceInfo detach(WorkspaceInfo workspace) {
        return detach(workspace, facade.detach(workspace));
    }

    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return facade.getDefaultWorkspace();
    }

    @Override
    public void setDefaultWorkspace(WorkspaceInfo defaultWorkspace) {
        if (defaultWorkspace != null) {
            WorkspaceInfo ws = facade.getWorkspaceByName(defaultWorkspace.getName());
            if (ws == null) {
                throw new IllegalArgumentException(
                        "No such workspace: '" + defaultWorkspace.getName() + "'");
            } else {
                defaultWorkspace = ws;
            }
        }
        WorkspaceInfo old = getDefaultWorkspace();
        // fire modify event before change
        fireModified(this, asList("defaultWorkspace"), asList(old), asList(defaultWorkspace));

        facade.setDefaultWorkspace(defaultWorkspace);

        // fire postmodify event after change
        firePostModified(this, asList("defaultWorkspace"), asList(old), asList(defaultWorkspace));
    }

    @Override
    public List<WorkspaceInfo> getWorkspaces() {
        return unmodifiableList(facade.getWorkspaces());
    }

    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return facade.getWorkspace(id);
    }

    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        if (name == null || Catalog.DEFAULT.equals(name)) {
            WorkspaceInfo ws = getDefaultWorkspace();
            if (ws != null) {
                name = ws.getName();
            }
        }
        return facade.getWorkspaceByName(name);
    }

    // Style methods
    @Override
    public StyleInfo getStyle(String id) {
        return facade.getStyle(id);
    }

    @Override
    public StyleInfo getStyleByName(String name) {
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

    @Override
    public StyleInfo getStyleByName(String workspaceName, String name) {
        if (workspaceName == null) {
            return getStyleByName((WorkspaceInfo) null, name);
        }

        WorkspaceInfo workspace = getWorkspaceByName(workspaceName);
        if (workspace != null) {
            return getStyleByName(workspace, name);
        }
        return null;
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        if (workspace == null) {
            workspace = DefaultCatalogFacade.NO_WORKSPACE;
        }
        return facade.getStyleByName(workspace, name);
    }

    @Override
    public List<StyleInfo> getStyles() {
        return unmodifiableList(facade.getStyles());
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(String workspaceName) {
        WorkspaceInfo workspace = null;
        if (workspaceName != null) {
            workspace = getWorkspaceByName(workspaceName);
            if (workspace == null) {
                return Collections.emptyList();
            }
        }

        return getStylesByWorkspace(workspace);
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return unmodifiableList(facade.getStylesByWorkspace(workspace));
    }

    @Override
    public void add(StyleInfo style) {
        doAdd(style, facade::add);
    }

    @Override
    public ValidationResult validate(StyleInfo style, boolean isNew) {
        return validationSupport.validate(style, isNew);
    }

    @Override
    public void remove(StyleInfo style) {
        doRemove(style, facade::remove);
    }

    @Override
    public void save(StyleInfo style) {
        doSave(style);
    }

    @Override
    public StyleInfo detach(StyleInfo style) {
        return detach(style, facade.detach(style));
    }

    // Event methods
    @Override
    public Collection<CatalogListener> getListeners() {
        return super.getListeners();
    }

    @Override
    public void addListener(CatalogListener listener) {
        super.addListener(listener);
    }

    @Override
    public void removeListener(CatalogListener listener) {
        super.removeListener(listener);
    }

    @Override
    public void removeListeners(Class listenerClass) {
        super.removeListeners(listenerClass);
    }

    @Override
    public ResourcePool getResourcePool() {
        return resourcePool;
    }

    @Override
    public void setResourcePool(ResourcePool resourcePool) {
        this.resourcePool = resourcePool;
    }

    @Override
    public GeoServerResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    @Override
    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void dispose() {
        if (resourcePool != null) resourcePool.dispose();
        facade.dispose();
    }

    // @Override TODO: add to the interface
    public void fireBeforeAdded(CatalogInfo object) {
        CatalogBeforeAddEventImpl event = new CatalogBeforeAddEventImpl();
        event.setSource(object);
        event(event);
    }

    @Override
    public void fireAdded(CatalogInfo object) {
        CatalogAddEventImpl event = new CatalogAddEventImpl();
        event.setSource(object);

        event(event);
    }

    @Override
    public void fireModified(
            CatalogInfo object, List propertyNames, List oldValues, List newValues) {
        CatalogModifyEventImpl event = new CatalogModifyEventImpl();

        event.setSource(object);
        event.setPropertyNames(propertyNames);
        event.setOldValues(oldValues);
        event.setNewValues(newValues);

        event(event);
    }

    @Override
    public void firePostModified(
            CatalogInfo object, List propertyNames, List oldValues, List newValues) {
        CatalogPostModifyEventImpl event = new CatalogPostModifyEventImpl();
        event.setSource(object);
        event.setPropertyNames(propertyNames);
        event.setOldValues(oldValues);
        event.setNewValues(newValues);
        event(event);
    }

    @Override
    public void fireRemoved(CatalogInfo object) {
        CatalogRemoveEventImpl event = new CatalogRemoveEventImpl();
        event.setSource(object);

        event(event);
    }

    @Override
    public void sync(CatalogImpl other) {
        other.getFacade().syncTo(facade);
        removeListeners(CatalogListener.class);
        other.getListeners().forEach(this::addListener);

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

    @Override
    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T extends CatalogInfo> int count(final Class<T> of, final Filter filter) {
        final CatalogFacade facade = getFacade();
        return facade.count(of, filter);
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            final Class<T> of, final Filter filter) {
        return list(of, filter, null, null, null);
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            final Class<T> of,
            final Filter filter,
            Integer offset,
            Integer count,
            SortBy sortOrder) {
        ExtendedCatalogFacade facade = getFacade();
        if (sortOrder != null
                && !facade.canSort(of, sortOrder.getPropertyName().getPropertyName())) {
            // TODO: use GeoTools' merge-sort code to provide sorting anyways
            throw new UnsupportedOperationException(
                    "Catalog backend can't sort on property "
                            + sortOrder.getPropertyName()
                            + " in-process sorting is pending implementation");
        }

        Query<T> query = Query.valueOf(of, filter, offset, count, sortOrder);
        Stream<T> stream = getFacade().query(query);
        return new CloseableIteratorAdapter<>(stream.iterator(), () -> stream.close());
    }

    public Optional<? extends CatalogInfo> findById(@NonNull String id) {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        final Filter filter = ff.id(ff.featureId(id));

        return Stream.of(
                        WorkspaceInfo.class,
                        NamespaceInfo.class,
                        StoreInfo.class,
                        ResourceInfo.class,
                        LayerInfo.class,
                        LayerGroupInfo.class,
                        StyleInfo.class,
                        MapInfo.class)
                .map(type -> get(type, filter))
                .filter(Objects::nonNull)
                .findFirst();
    }

    @Override
    public <T extends CatalogInfo> T get(Class<T> type, Filter filter)
            throws IllegalArgumentException {
        // try optimizing by querying by id first, defer to regular filter query if filter is not
        // and Id filter
        return getIdIfIdFilter(filter) //
                .map(id -> findOneById(id, type)) //
                .or(() -> findOne(type, filter)) //
                .orElse(null);
    }

    private <T extends CatalogInfo> Optional<T> findOne(Class<T> type, Filter filter) {
        Query<T> query = Query.valueOf(type, filter, 0, 2);
        try (Stream<T> stream = getFacade().query(query)) {
            List<T> matches = stream.limit(2).toList();
            switch (matches.size()) {
                case 0:
                    return Optional.empty();
                case 1:
                    return Optional.of(matches.get(0));
                default:
                    throw new IllegalArgumentException(
                            "Specified query predicate resulted in more than one object");
            }
        }
    }

    private <T extends CatalogInfo> T findOneById(String id, Class<T> type) {
        final ClassMappings cm = classMapping(type).orElseThrow();
        switch (cm) {
            case NAMESPACE:
                return type.cast(getNamespace(id));
            case WORKSPACE:
                return type.cast(getWorkspace(id));
            case STORE:
                return type.cast(getStore(id, StoreInfo.class));
            case COVERAGESTORE:
                return type.cast(getCoverageStore(id));
            case DATASTORE:
                return type.cast(getDataStore(id));
            case WMSSTORE:
                return type.cast(getStore(id, WMSStoreInfo.class));
            case WMTSSTORE:
                return type.cast(getStore(id, WMTSStoreInfo.class));
            case RESOURCE:
                return type.cast(getResource(id, ResourceInfo.class));
            case COVERAGE:
                return type.cast(getCoverage(id));
            case FEATURETYPE:
                return type.cast(getFeatureType(id));
            case WMSLAYER:
                return type.cast(getResource(id, WMSLayerInfo.class));
            case WMTSLAYER:
                return type.cast(getResource(id, WMTSLayerInfo.class));
            case LAYER:
                return type.cast(getLayer(id));
            case LAYERGROUP:
                return type.cast(getLayerGroup(id));
            case PUBLISHED:
                return type.cast(
                        Optional.<PublishedInfo>ofNullable(getLayer(id))
                                .orElseGet(() -> getLayerGroup(id)));
            case STYLE:
                return type.cast(getStyle(id));
            default:
                throw new IllegalArgumentException("Unexpected value: " + cm);
        }
    }

    private Optional<String> getIdIfIdFilter(Filter filter) {
        String id = null;
        if (filter instanceof Id idFilter) {
            Set<Identifier> identifiers = idFilter.getIdentifiers();
            if (identifiers.size() == 1) {
                id = identifiers.iterator().next().toString();
            }
        } else if (filter instanceof PropertyIsEqualTo eq) {
            boolean idProperty =
                    (eq.getExpression1() instanceof PropertyName)
                            && "id".equals(((PropertyName) eq.getExpression1()).getPropertyName());
            if (idProperty && eq.getExpression2() instanceof Literal) {
                id = Converters.convert(eq.getExpression2().evaluate(null), String.class);
            }
        }
        return Optional.ofNullable(id);
    }

    protected <T extends CatalogInfo> Optional<ClassMappings> classMapping(Class<T> type) {
        return Optional.ofNullable(ClassMappings.fromInterface(type))
                .or(() -> Optional.ofNullable(ClassMappings.fromImpl(type)));
    }

    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        return facade.getCatalogCapabilities();
    }

    protected <T extends CatalogInfo> void doAdd(T object, Function<T, T> inserter) {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(inserter, "insert function");
        setId(object);
        validationSupport.validate(object, true);

        CatalogOpContext<T> context = new CatalogOpContext<>(this, object);
        businessRules.onBeforeAdd(context);
        fireBeforeAdded(object);
        try {
            T added = inserter.apply(object);
            // fire the event before the post-rules are processed, since they may result in
            // other objects removed/modified, and hence avoid a secondary event to be notified
            // before the primary one. For example, a post-rule may result in a call to
            // setDefaultWorspace/Namespace/DataStore
            fireAdded(added);
            businessRules.onAfterAdd(context.setObject(added));
        } catch (RuntimeException error) {
            businessRules.onAfterAdd(context.setError(error));
            throw error;
        }
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
        final ModificationProxy proxy = ProxyUtils.handler(info, ModificationProxy.class);
        if (null == proxy) {
            throw new IllegalArgumentException(
                    "The object to save ("
                            + info.getClass().getName()
                            + ") is not a ModificationProxy and hence did not come out of this catalog. Saving an object requires to use an instance obtained from the Catalog.");
        }
        validationSupport.validate(info, false);

        // use the proxied object, may some listener change it
        fireModified(info, proxy.getPropertyNames(), proxy.getOldValues(), proxy.getNewValues());

        // this could be the event's payload instead of three separate lists
        PropertyDiff diff = PropertyDiff.valueOf(proxy).clean();

        CatalogOpContext<I> context = new CatalogOpContext<>(this, info, diff);
        businessRules.onBeforeSave(context);
        // recompute diff in case a business rule changed info
        diff = PropertyDiff.valueOf(proxy).clean();
        context.setDiff(diff);

        final List<String> propertyNames = diff.getPropertyNames();
        final List<Object> oldValues = diff.getOldValues();
        final List<Object> newValues = diff.getNewValues();
        final Patch patch = diff.toPatch();
        try {
            // note info will be unwrapped before being given to the raw facade by the inbound
            // resolving function set at #setFacade
            ExtendedCatalogFacade facade = getFacade();
            I updated = facade.update(info, patch);

            // commit proxy, making effective the change in the provided object. Has no effect in
            // what's been passed to the facade
            proxy.commit();
            // fire the event before the post-rules are processed, since they may result in other
            // objects removed/modified, and hence avoid a secondary event to be notified before the
            // primary one. For example, a post-rule may result in a call to
            // setDefaultWorspace/Namespace/DataStore
            firePostModified(updated, propertyNames, oldValues, newValues);
            businessRules.onAfterSave(context.setObject(updated));
        } catch (RuntimeException error) {
            businessRules.onAfterSave(context.setError(error));
            throw error;
        }
    }

    protected <T extends CatalogInfo> void doRemove(T object, Consumer<T> remover) {
        validationSupport.beforeRemove(object);
        CatalogOpContext<T> context = new CatalogOpContext<>(this, object);

        businessRules.onBeforeRemove(context);
        try {
            remover.accept(object);
            // fire the event before the post-rules are processed, since they may result in
            // other objects removed/modified, and hence avoid a secondary event to be notified
            // before the primary one. For example, a post-rule may result in a call to
            // setDefaultWorspace/Namespace/DataStore
            fireRemoved(object);
            businessRules.onRemoved(context);
        } catch (RuntimeException error) {
            businessRules.onRemoved(context.setError(error));
            throw error;
        }
    }

    private <T extends CatalogInfo> T detach(T original, T detached) {
        return detached != null ? detached : original;
    }

    protected void setId(CatalogInfo o) {
        if (null == o.getId()) {
            String uid = UUID.randomUUID().toString();
            String id = o.getClass().getSimpleName() + "-" + uid;
            OwsUtils.set(o, "id", id);
        } else {
            LOGGER.fine(String.format("Using user provided id %s", o.getId()));
        }
    }

    // if value is null, the list is a singleton list with a null member
    private <T> List<T> asList(@Nullable T value) {
        return Collections.singletonList(value);
    }

    public void save(CatalogInfo info) {
        doSave(info);
    }

    public void remove(CatalogInfo info) {
        ClassMappings cm =
                classMapping(ModificationProxy.unwrap((CatalogInfo) info).getClass()).orElseThrow();
        switch (cm) {
            case WORKSPACE:
                remove((WorkspaceInfo) info);
                break;
            case NAMESPACE:
                remove((NamespaceInfo) info);
                break;
            case STORE:
            case COVERAGESTORE:
            case DATASTORE:
            case WMSSTORE:
            case WMTSSTORE:
                remove((StoreInfo) info);
                break;
            case RESOURCE:
            case FEATURETYPE:
            case COVERAGE:
            case WMSLAYER:
            case WMTSLAYER:
                remove((ResourceInfo) info);
                break;
            case LAYER:
                remove((LayerInfo) info);
                break;
            case LAYERGROUP:
                remove((LayerGroupInfo) info);
                break;
            case STYLE:
                remove((StyleInfo) info);
                break;
            case MAP:
                remove((MapInfo) info);
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + cm);
        }
    }
}
