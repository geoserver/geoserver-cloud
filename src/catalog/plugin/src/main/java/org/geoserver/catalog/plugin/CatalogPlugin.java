/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static java.util.Collections.unmodifiableList;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogListener;
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
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.identity.Identifier;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;

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
@SuppressWarnings("serial")
public class CatalogPlugin extends CatalogImpl implements Catalog {

    /** logger */
    private static final Logger LOGGER = Logging.getLogger(CatalogPlugin.class);

    /** Handles {@link CatalogInfo} validation rules before adding or updating an object */
    protected final transient CatalogValidationRules validationSupport;

    private final transient CatalogBusinessRules businessRules = new CatalogBusinessRules();

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
    protected CatalogPlugin(CatalogPlugin catalog) {
        super(catalog); // sets dispatcher and resourcePool
        this.isolated = false;
        this.validationSupport = new CatalogValidationRules(this);
        super.resourceLoader = catalog.getResourceLoader();
        super.rawFacade = catalog.getRawFacade();
        super.facade = catalog.getFacade();
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
    @Override
    public void setExtendedValidation(boolean extendedValidation) {
        validationSupport.setExtendedValidation(extendedValidation);
    }

    @Override
    public boolean isExtendedValidation() {
        return validationSupport.isExtendedValidation();
    }

    @Override
    public void setFacade(CatalogFacade facade) {
        // Original data access facade provided at {@link #setFacade(CatalogFacade)}, may or may be
        // not  a {@link ResolvingCatalogFacadeDecorator}. If not, {@link #facade} will be a
        // resolving decorator to allow traits to be added.
        super.rawFacade = facade;
        ExtendedCatalogFacade efacade;
        UnaryOperator<CatalogInfo> outboundResolver;
        UnaryOperator<CatalogInfo> inboundResolver;
        if (facade instanceof ExtendedCatalogFacade extended) {
            efacade = extended;
            // make sure no object leaves the catalog without being proxied, nor enters the
            // facade as a proxy. Note it is ok if the provided facade is already a
            // ResolvingCatalogFacade.
            // This catalog doesn't care which object resolution chain the provided facade
            // needs to perform.
            outboundResolver = ModificationProxyDecorator::wrap;
            inboundResolver = ModificationProxyDecorator::unwrap;
        } else {
            efacade = new CatalogFacadeExtensionAdapter(facade);
            outboundResolver = UnaryOperator.identity();
            inboundResolver = UnaryOperator.identity();
        }
        // decorate the default catalog facade with one capable of handling isolated
        // workspaces
        if (this.isolated) {
            efacade = new IsolatedCatalogFacade(efacade);
        }
        ResolvingCatalogFacadeDecorator resolving = new ResolvingCatalogFacadeDecorator(efacade);
        resolving.setOutboundResolver(outboundResolver);
        resolving.setInboundResolver(inboundResolver);
        super.facade = resolving;
        super.facade.setCatalog(this);
    }

    public void add(@NonNull CatalogInfo info) {
        switch (info) {
            case WorkspaceInfo ws -> add(ws);
            case NamespaceInfo ns -> add(ns);
            case StoreInfo st -> add(st);
            case ResourceInfo r -> add(r);
            case LayerInfo l -> add(l);
            case LayerGroupInfo lg -> add(lg);
            case StyleInfo s -> add(s);
            case MapInfo m -> add(m);
            default -> throw new IllegalArgumentException("Unexpected value: %s"
                    .formatted(ModificationProxy.unwrap(info).getClass()));
        }
    }

    public void save(@NonNull CatalogInfo info) {
        doSave(info);
    }

    public void remove(@NonNull CatalogInfo info) {
        switch (info) {
            case WorkspaceInfo ws -> remove(ws);
            case NamespaceInfo ns -> remove(ns);
            case StoreInfo st -> remove(st);
            case ResourceInfo r -> remove(r);
            case LayerInfo l -> remove(l);
            case LayerGroupInfo lg -> remove(lg);
            case StyleInfo s -> remove(s);
            case MapInfo m -> remove(m);
            default -> throw new IllegalArgumentException("Unexpected value: %s"
                    .formatted(ModificationProxy.unwrap(info).getClass()));
        }
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
        doRemove(store, StoreInfo.class);
    }

    /**
     * Overrides with same logic as {@link CatalogImpl#save(StoreInfo)} but calls {@link
     * #doSave(CatalogInfo) doSave(store)} instead of {@link CatalogFacade#save(StoreInfo)
     * facade.save(store)}
     */
    @Override
    public void save(StoreInfo store) {
        if (store.getId() == null) {
            // some code uses save() when it should use add()
            add(store);
            return;
        }

        validate(store, false);

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
                    newNamespace -> updateResourcesNamespace(store, oldWorkspace.getName(), newNamespace));
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
            List<ResourceInfo> storeResources, String oldNamespacePrefix, NamespaceInfo newNamespace) {
        // rolback the namespace on all the resources that got it changed
        final NamespaceInfo rolbackNamespace = getNamespaceByPrefix(oldNamespacePrefix);
        Objects.requireNonNull(rolbackNamespace);

        final String newNsId = newNamespace.getId();
        storeResources.stream()
                .filter(r -> newNsId.equals(r.getNamespace().getId()))
                .forEach(resource -> updateNamespace(resource, rolbackNamespace));
    }

    // override, super calls facade.save and depends on it throwing the events
    protected @Override void rollback(StoreInfo store, StoreInfo rollbackTo) {
        // apply the rollback object properties to the real store
        OwsUtils.copy(rollbackTo, store, infoInterfaceOf(rollbackTo));
        doSave(store);
    }

    /**
     * Overriding because super calls facade.save and depends on it throwing the events. Calling
     * {@link #doSave(CatalogInfo)} instead to take responsibility of validation here.
     */
    @Override
    public void updateNamespace(ResourceInfo resource, NamespaceInfo newNamespace) {
        resource.setNamespace(newNamespace);
        doSave(resource);
    }

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(WorkspaceInfo workspace, Class<T> clazz) {

        return unmodifiableList(facade.getStoresByWorkspace(workspace, clazz));
    }

    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return unmodifiableList(facade.getStores(clazz));
    }

    /**
     * Overridden to fire pre and post-modified events with this catalog as the event source, and
     * {@literal defaultDataStore} as the modified property
     */
    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        if (store != null) {
            // basic sanity check
            if (store.getWorkspace() == null) {
                throw new IllegalArgumentException("The store has not been assigned a workspace");
            }

            if (!store.getWorkspace().equals(workspace)) {
                throw new IllegalArgumentException(
                        "Trying to mark as default for workspace %s a store that is contained in %s"
                                .formatted(
                                        workspace.getName(),
                                        store.getWorkspace().getName()));
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
        doRemove(resource, ResourceInfo.class);
    }

    @Override
    public void save(ResourceInfo resource) {
        doSave(resource);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return unmodifiableList(facade.getResources(clazz));
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(NamespaceInfo namespace, Class<T> clazz) {
        return unmodifiableList(facade.getResourcesByNamespace(namespace, clazz));
    }

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
        doRemove(layer, LayerInfo.class);
    }

    @Override
    public void save(LayerInfo layer) {
        doSave(layer);
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
        doRemove(layerGroup, LayerGroupInfo.class);
    }

    @Override
    public void save(LayerGroupInfo layerGroup) {
        doSave(layerGroup);
    }

    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return unmodifiableList(facade.getLayerGroups());
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return unmodifiableList(facade.getLayerGroupsByWorkspace(workspace));
    }

    @Override
    public void add(MapInfo map) {
        doAdd(map, facade::add);
    }

    @Override
    public void remove(MapInfo map) {
        doRemove(map, MapInfo.class);
    }

    @Override
    public void save(MapInfo map) {
        doSave(map);
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
        doRemove(namespace, NamespaceInfo.class);
    }

    @Override
    public void save(NamespaceInfo namespace) {
        doSave(namespace);
    }

    /**
     * Overridden to fire pre and post-modified events with this catalog as the event source, and
     * {@literal defaultNamespace} as the modified property
     */
    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        if (defaultNamespace != null) {
            NamespaceInfo ns = getNamespaceByPrefix(defaultNamespace.getPrefix());
            if (ns == null) {
                throw new IllegalArgumentException("No such namespace: '%s'".formatted(defaultNamespace.getPrefix()));
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
        doRemove(workspace, WorkspaceInfo.class);
    }

    @Override
    public void save(WorkspaceInfo workspace) {
        doSave(workspace);
    }

    /**
     * Overridden to fire pre and post-modified events with this catalog as the event source, and
     * {@literal defaultWorkspace} as the modified property
     */
    @Override
    public void setDefaultWorkspace(WorkspaceInfo defaultWorkspace) {
        if (defaultWorkspace != null) {
            WorkspaceInfo ws = facade.getWorkspaceByName(defaultWorkspace.getName());
            if (ws == null) {
                throw new IllegalArgumentException("No such workspace: '%s'".formatted(defaultWorkspace.getName()));
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
    public List<StyleInfo> getStyles() {
        return unmodifiableList(facade.getStyles());
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
        doRemove(style, StyleInfo.class);
    }

    @Override
    public void save(StyleInfo style) {
        doSave(style);
    }

    @Override
    public void sync(CatalogImpl other) {
        other.getFacade().syncTo(facade);
        removeListeners(CatalogListener.class);
        other.getListeners().forEach(this::addListener);

        ResourcePool resourcePool = other.getResourcePool();
        // REVISIT: this still sounds wrong... looks like an old assumption that both
        // catalogs are in-memory catalogs and the argument one is to be disposed, which in the end
        // means this method does not belong here but to whom's calling (DefaultGeoServerLoader?)
        if (this.resourcePool != resourcePool) {
            this.resourcePool.dispose();
        }
        setResourcePool(resourcePool);
        resourcePool.setCatalog(this);
        resourceLoader = other.getResourceLoader();
    }

    /** Overrides to call {@link ExtendedCatalogFacade#query(Query)} */
    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            final Class<T> of, final Filter filter, Integer offset, Integer count, SortBy sortOrder) {
        ExtendedCatalogFacade facade = getFacade();
        if (sortOrder != null && !facade.canSort(of, sortOrder.getPropertyName().getPropertyName())) {
            // TODO: use GeoTools' merge-sort code to provide sorting anyways
            throw new UnsupportedOperationException(
                    "Catalog backend can't sort on property %s in-process sorting is pending implementation"
                            .formatted(sortOrder.getPropertyName()));
        }

        Query<T> query = Query.valueOf(of, filter, offset, count, sortOrder);
        Stream<T> stream = getFacade().query(query);
        return new CloseableIteratorAdapter<>(stream.iterator(), stream::close);
    }

    public Optional<CatalogInfo> findById(@NonNull String id) {
        return getFacade().get(id);
    }

    @Override
    public <T extends CatalogInfo> T get(Class<T> type, Filter filter) throws IllegalArgumentException {
        // try optimizing by querying by id first, defer to regular filter query if
        // filter is not and Id filter
        return getIdIfIdFilter(filter) //
                .flatMap(id -> findById(id, type)) //
                .or(() -> findOne(type, filter)) //
                .orElse(null);
    }

    private <T extends CatalogInfo> Optional<T> findOne(Class<T> type, Filter filter) {
        Query<T> query = Query.valueOf(type, filter, 0, 2);
        try (Stream<T> stream = getFacade().query(query)) {
            List<T> matches = stream.limit(2).toList();
            return switch (matches.size()) {
                case 0 -> Optional.empty();
                case 1 -> Optional.of(matches.get(0));
                default -> throw new IllegalArgumentException(
                        "Specified query predicate resulted in more than one object");
            };
        }
    }

    public <T extends CatalogInfo> Optional<T> findById(String id, Class<T> type) {
        return getFacade().get(id, type);
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
                    (eq.getExpression1() instanceof PropertyName prop) && "id".equals(prop.getPropertyName());
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

    protected <T extends CatalogInfo> void doAdd(T object, UnaryOperator<T> inserter) {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(inserter, "insert function");
        setId(object);
        validationSupport.validate(object, true);

        CatalogOpContext<T> context = new CatalogOpContext<>(this, object);
        businessRules.onBeforeAdd(context);
        fireBeforeAdded(object);
        try {
            T added = inserter.apply(object);
            // fire the event before the post-rules are processed, since they may result in other
            // objects removed/modified, and hence avoid a secondary event to be notifiedbefore the
            // primary one. For example, a post-rule may result in a call to
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
                    """
                    The object to save (%s)
                    is not a ModificationProxy and hence did not come out of this catalog.
                    Saving an object requires to use an instance obtained from the Catalog.
                    """
                            .formatted(info.getClass().getName()));
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
            // note info will be unwrapped before being given to the raw facade by the
            // inbound resolving function set at #setFacade
            ExtendedCatalogFacade facade = getFacade();
            I updated = facade.update(info, patch);

            // commit proxy, making effective the change in the provided object. Has no
            // effect in what's been passed to the facade
            proxy.commit();
            // fire the event before the post-rules are processed, since they may result in
            // other objects removed/modified, and hence avoid a secondary event to be notified
            // before the primary one. For example, a post-rule may result in a call to
            // setDefaultWorspace/Namespace/DataStore
            firePostModified(updated, propertyNames, oldValues, newValues);
            businessRules.onAfterSave(context.setObject(updated));
        } catch (RuntimeException error) {
            businessRules.onAfterSave(context.setError(error));
            throw error;
        }
    }

    protected <T extends CatalogInfo> void doRemove(T object, Class<T> type) {
        // proceed only if found, avoid no-op events
        getFacade().get(object.getId(), type).ifPresent(this::doRemove);
    }

    private <T extends CatalogInfo> void doRemove(T found) {
        validationSupport.beforeRemove(found);
        CatalogOpContext<T> context = new CatalogOpContext<>(this, found);

        businessRules.onBeforeRemove(context);
        try {
            getFacade().remove(ModificationProxy.unwrap(found));
            // fire the event before the post-rules are processed, since they may result in other
            // objects removed/modified, and hence avoid a secondary event to be notified before the
            // primary one. For example, a post-rule may result in a call to
            // setDefaultWorspace/Namespace/DataStore
            fireRemoved(found);
            businessRules.onRemoved(context);
        } catch (RuntimeException error) {
            businessRules.onRemoved(context.setError(error));
            throw error;
        }
    }

    protected void setId(@NonNull CatalogInfo o) {
        if (null == o.getId()) {
            String type = ClassMappings.fromImpl(ModificationProxy.unwrap(o).getClass())
                    .getInterface()
                    .getSimpleName();
            Ulid ulid = UlidCreator.getMonotonicUlid();
            String id = "%s-%s".formatted(type, ulid);
            OwsUtils.set(o, "id", id);
        } else if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Using provided id %s".formatted(o.getId()));
        }
    }

    // if value is null, the list is a singleton list with a null member
    private <T> List<T> asList(@Nullable T value) {
        return Collections.singletonList(value);
    }
}
