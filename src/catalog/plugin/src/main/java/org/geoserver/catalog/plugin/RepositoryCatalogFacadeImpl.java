/*
 * (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans This
 * code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static java.lang.String.format;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogCapabilities;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LockingCatalogFacade;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ProxyUtils;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.springframework.util.Assert;

import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class RepositoryCatalogFacadeImpl extends CatalogInfoRepositoryHolderImpl
        implements RepositoryCatalogFacade {

    protected Catalog catalog;

    protected final CatalogCapabilities capabilities = new CatalogCapabilities();

    public RepositoryCatalogFacadeImpl() {}

    public RepositoryCatalogFacadeImpl(Catalog catalog) {
        this();
        setCatalog(catalog);
    }

    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        return capabilities;
    }

    @Override
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Catalog getCatalog() {
        return catalog;
    }

    @Override
    public void resolve() {
        // no-op, override as appropriate
    }

    protected <I extends CatalogInfo> I add(
            I info, Class<I> type, CatalogInfoRepository<I> repository) {
        checkNotAProxy(info);
        Objects.requireNonNull(info.getId(), "Object id not provided");
        repository.add(info);
        return repository.findById(info.getId(), type).orElse(null);
    }

    //
    // Stores
    //
    @Override
    public StoreInfo add(StoreInfo store) {
        return add(store, StoreInfo.class, stores);
    }

    @Override
    public void remove(StoreInfo store) {
        stores.remove(store);
    }

    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return stores.findById(id, clazz).orElse(null);
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {

        Optional<T> result;
        if (workspace == ANY_WORKSPACE || workspace == null) {
            result = stores.findFirstByName(name, clazz);
        } else {
            result = stores.findByNameAndWorkspace(name, workspace, clazz);
        }
        return result.orElse(null);
    }

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        // Question: do we need to support ANY_WORKSPACE? see "todo" comment in DefaultCatalogFacade
        final WorkspaceInfo ws;
        if (workspace == null) {
            ws = getDefaultWorkspace();
        } else {
            ws = workspace;
        }

        return toList(() -> stores.findAllByWorkspace(ws, clazz));
    }

    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return toList(() -> stores.findAllByType(clazz));
    }

    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return stores.getDefaultDataStore(workspace).orElse(null);
    }

    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        if (store != null) {
            Objects.requireNonNull(store.getWorkspace());
            Assert.isTrue(
                    workspace.getId().equals(store.getWorkspace().getId()),
                    "Store workspace mismatch");
        }

        if (store == null) stores.unsetDefaultDataStore(workspace);
        else stores.setDefaultDataStore(workspace, store);
    }

    //
    // Resources
    //
    @Override
    public ResourceInfo add(ResourceInfo resource) {
        return add(resource, ResourceInfo.class, resources);
    }

    @Override
    public void remove(ResourceInfo resource) {
        resources.remove(resource);
    }

    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return resources.findById(id, clazz).orElse(null);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo namespace, String name, Class<T> clazz) {
        Optional<T> result;
        if (namespace == ANY_NAMESPACE) {
            result = resources.findFirstByName(name, clazz);
        } else {
            result = resources.findByNameAndNamespace(name, namespace, clazz);
        }

        return result.orElse(null);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return toList(() -> resources.findAllByType(clazz));
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        // Question: do we need to support ANY_WORKSPACE? see "todo" comment in DefaultCatalogFacade
        NamespaceInfo ns = namespace == null ? getDefaultNamespace() : namespace;
        return toList(() -> resources.findAllByNamespace(ns, clazz));
    }

    @Override
    public <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        Optional<T> resource;
        NamespaceInfo ns = null;
        if (store.getWorkspace() != null
                && store.getWorkspace().getName() != null
                && (ns = getNamespaceByPrefix(store.getWorkspace().getName())) != null) {

            resource = resources.findByNameAndNamespace(name, ns, clazz);
            if (resource.isPresent() && !(store.equals(resource.get().getStore()))) {
                return null;
            }
        } else {
            // should not happen, but some broken test code sets up namespaces without equivalent
            // workspaces
            // or stores without workspaces
            resource = resources.findByStoreAndName(store, name, clazz);
        }
        return resource.orElse(null);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return toList(() -> resources.findAllByStore(store, clazz));
    }

    //
    // Layers
    //
    @Override
    public LayerInfo add(LayerInfo layer) {
        return add(layer, LayerInfo.class, layers);
    }

    @Override
    public void remove(LayerInfo layer) {
        layers.remove(layer);
    }

    @Override
    public LayerInfo getLayer(String id) {
        return layers.findById(id, LayerInfo.class).orElse(null);
    }

    @Override
    public LayerInfo getLayerByName(String name) {
        return layers.findOneByName(name).orElse(null);
    }

    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return toList(() -> layers.findAllByResource(resource));
    }

    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {
        return toList(() -> layers.findAllByDefaultStyleOrStyles(style));
    }

    @Override
    public List<LayerInfo> getLayers() {
        return toList(layers::findAll);
    }

    //
    // Maps
    //
    @Override
    public MapInfo add(MapInfo map) {
        return add(map, MapInfo.class, maps);
    }

    @Override
    public void remove(MapInfo map) {
        maps.remove(map);
    }

    @Override
    public MapInfo getMap(String id) {
        return maps.findById(id, MapInfo.class).orElse(null);
    }

    @Override
    public MapInfo getMapByName(String name) {
        return maps.findFirstByName(name, MapInfo.class).orElse(null);
    }

    @Override
    public List<MapInfo> getMaps() {
        return toList(maps::findAll);
    }

    //
    // Layer groups
    //
    @Override
    public LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return add(layerGroup, LayerGroupInfo.class, getLayerGroupRepository());
    }

    @Override
    public void remove(LayerGroupInfo layerGroup) {
        getLayerGroupRepository().remove(layerGroup);
    }

    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return toList(getLayerGroupRepository()::findAll);
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        // Question: do we need to support ANY_WORKSPACE? see "todo" comment in DefaultCatalogFacade

        WorkspaceInfo ws;
        if (workspace == null) {
            ws = getDefaultWorkspace();
        } else {
            ws = workspace;
        }
        Stream<LayerGroupInfo> matches;
        if (workspace == NO_WORKSPACE) {
            matches = getLayerGroupRepository().findAllByWorkspaceIsNull();
        } else {
            matches = getLayerGroupRepository().findAllByWorkspace(ws);
        }
        return toList(() -> matches);
    }

    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return getLayerGroupRepository().findById(id, LayerGroupInfo.class).orElse(null);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return getLayerGroupByName(NO_WORKSPACE, name);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {

        if (workspace == NO_WORKSPACE)
            return getLayerGroupRepository().findByNameAndWorkspaceIsNull(name).orElse(null);

        if (ANY_WORKSPACE == workspace)
            return getLayerGroupRepository()
                    .findFirstByName(name, LayerGroupInfo.class)
                    .orElse(null);

        return getLayerGroupRepository().findByNameAndWorkspace(name, workspace).orElse(null);
    }

    //
    // Namespaces
    //
    @Override
    public NamespaceInfo add(NamespaceInfo namespace) {
        return add(namespace, NamespaceInfo.class, namespaces);
    }

    @Override
    public void remove(NamespaceInfo namespace) {
        NamespaceInfo defaultNamespace = getDefaultNamespace();
        if (defaultNamespace != null && namespace.getId().equals(defaultNamespace.getId())) {
            setDefaultNamespace(null);
        }
        namespaces.remove(namespace);
    }

    @Override
    public NamespaceInfo getDefaultNamespace() {
        return namespaces.getDefaultNamespace().orElse(null);
    }

    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamnespace) {
        if (defaultNamnespace == null) namespaces.unsetDefaultNamespace();
        else namespaces.setDefaultNamespace(defaultNamnespace);
    }

    @Override
    public NamespaceInfo getNamespace(String id) {
        return namespaces.findById(id, NamespaceInfo.class).orElse(null);
    }

    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return namespaces.findFirstByName(prefix, NamespaceInfo.class).orElse(null);
    }

    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        return namespaces.findOneByURI(uri).orElse(null);
    }

    @Override
    public List<NamespaceInfo> getNamespacesByURI(String uri) {
        return toList(() -> namespaces.findAllByURI(uri));
    }

    @Override
    public List<NamespaceInfo> getNamespaces() {
        return toList(namespaces::findAll);
    }

    //
    // Workspaces
    //
    // Workspace methods
    @Override
    public WorkspaceInfo add(WorkspaceInfo workspace) {
        return add(workspace, WorkspaceInfo.class, workspaces);
    }

    @Override
    public void remove(WorkspaceInfo workspace) {
        WorkspaceInfo defaultWorkspace = getDefaultWorkspace();
        if (defaultWorkspace != null && workspace.getId().equals(defaultWorkspace.getId())) {
            workspaces.unsetDefaultWorkspace();
        }
        workspaces.remove(workspace);
    }

    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return workspaces.getDefaultWorkspace().orElse(null);
    }

    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        WorkspaceInfo ws = workspace;
        if (ws == null) workspaces.unsetDefaultWorkspace();
        else workspaces.setDefaultWorkspace(ws);
    }

    @Override
    public List<WorkspaceInfo> getWorkspaces() {
        return toList(workspaces::findAll);
    }

    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return workspaces.findById(id, WorkspaceInfo.class).orElse(null);
    }

    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        return workspaces.findFirstByName(name, WorkspaceInfo.class).orElse(null);
    }

    //
    // Styles
    //
    @Override
    public StyleInfo add(StyleInfo style) {
        return add(style, StyleInfo.class, styles);
    }

    @Override
    public void remove(StyleInfo style) {
        styles.remove(style);
    }

    @Override
    public StyleInfo getStyle(String id) {
        return styles.findById(id, StyleInfo.class).orElse(null);
    }

    @Override
    public StyleInfo getStyleByName(String name) {
        Optional<StyleInfo> match = styles.findByNameAndWordkspaceNull(name);
        if (match.isEmpty()) {
            match = styles.findFirstByName(name, StyleInfo.class);
        }
        return match.orElse(null);
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        Objects.requireNonNull(workspace, "workspace");
        Objects.requireNonNull(name, "name");

        if (workspace == ANY_WORKSPACE) {
            return getStyleByName(name);
        }
        Optional<StyleInfo> match;
        if (workspace == NO_WORKSPACE) {
            match = styles.findByNameAndWordkspaceNull(name);
        } else {
            match = styles.findByNameAndWorkspace(name, workspace);
        }
        return match.orElse(null);
    }

    @Override
    public List<StyleInfo> getStyles() {
        return toList(styles::findAll);
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        // Question: do we need to support ANY_WORKSPACE? see "todo" comment in DefaultCatalogFacade
        Stream<StyleInfo> matches;
        if (workspace == NO_WORKSPACE) {
            matches = styles.findAllByNullWorkspace();
        } else {
            WorkspaceInfo ws;
            if (workspace == null) {
                ws = getDefaultWorkspace();
            } else {
                ws = workspace;
            }

            matches = styles.findAllByWorkspace(ws);
        }
        return toList(() -> matches);
    }

    protected <T extends CatalogInfo> List<T> toList(Supplier<Stream<T>> supplier) {
        try (Stream<T> stream = supplier.get()) {
            return stream.toList();
        }
    }

    @Override
    public void dispose() {
        dispose(stores);
        dispose(resources);
        dispose(namespaces);
        dispose(workspaces);
        dispose(layers);
        dispose(layerGroups);
        dispose(maps);
        dispose(styles);
    }

    private void dispose(CatalogInfoRepository<?> repository) {
        if (repository != null) repository.dispose();
    }

    @Override
    public void syncTo(CatalogFacade to) {
        final CatalogFacade dao = ProxyUtils.unwrap(to, LockingCatalogFacade.class);
        if (dao instanceof CatalogInfoRepositoryHolder other) {
            // do an optimized sync
            this.workspaces.syncTo(other.getWorkspaceRepository());
            this.namespaces.syncTo(other.getNamespaceRepository());
            this.stores.syncTo(other.getStoreRepository());
            this.resources.syncTo(other.getResourceRepository());
            this.layers.syncTo(other.getLayerRepository());
            this.layerGroups.syncTo(other.getLayerGroupRepository());
            this.styles.syncTo(other.getStyleRepository());
            this.maps.syncTo(other.getMapRepository());
            dao.setCatalog(catalog);
        } else {
            // do a manual import
            sync(workspaces::findAll, dao::add);
            sync(namespaces::findAll, dao::add);
            sync(stores::findAll, dao::add);
            sync(resources::findAll, dao::add);
            sync(styles::findAll, dao::add);
            sync(layers::findAll, dao::add);
            sync(layerGroups::findAll, dao::add);
            sync(maps::findAll, dao::add);
        }

        dao.setDefaultWorkspace(getDefaultWorkspace());
        dao.setDefaultNamespace(getDefaultNamespace());
        try (Stream<DataStoreInfo> defaultDataStores = stores.getDefaultDataStores()) {
            defaultDataStores.forEach(d -> dao.setDefaultDataStore(d.getWorkspace(), d));
        }
    }

    private <T extends CatalogInfo> void sync(Supplier<Stream<T>> from, Consumer<T> to) {
        try (Stream<T> all = from.get()) {
            all.forEach(to::accept);
        }
    }

    @Override
    public <T extends CatalogInfo> int count(final Class<T> of, final Filter filter) {
        long count;
        if (PublishedInfo.class.equals(of)) {
            long layers = count(LayerInfo.class, filter);
            long groups = count(LayerGroupInfo.class, filter);
            count = layers + groups;
        } else {
            try {
                count = repository(of).count(of, filter);
            } catch (RuntimeException e) {
                throw new CatalogException(
                        "Error obtaining count of %s with filter %s"
                                .formatted(of.getSimpleName(), filter),
                        e);
            }
        }
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    /**
     * This default implementation supports sorting against properties (could be nested) that are
     * either of a primitive type or implement {@link Comparable}.
     *
     * @param type the type of object to sort
     * @param propertyName the property name of the objects of type {@code type} to sort by
     * @see CatalogInfoRepository#canSortBy(String)
     */
    @Override
    public boolean canSort(final Class<? extends CatalogInfo> type, final String propertyName) {
        if (PublishedInfo.class.equals(type)) {
            return canSort(LayerInfo.class, propertyName)
                    || canSort(LayerGroupInfo.class, propertyName);
        }
        return repository(type).canSortBy(propertyName);
    }

    private <T extends CatalogInfo> void checkCanSort(final Query<T> query) {
        query.getSortBy().forEach(sb -> checkCanSort(query.getType(), sb));
    }

    private <T extends CatalogInfo> void checkCanSort(final Class<T> type, SortBy order) {
        if (!canSort(type, order.getPropertyName().getPropertyName())) {
            throw new IllegalArgumentException(
                    format(
                            "Can't sort objects of type %s by %s",
                            type.getName(), order.getPropertyName()));
        }
    }

    @Override
    public <T extends CatalogInfo> Stream<T> query(Query<T> query) {
        Stream<T> stream;
        if (PublishedInfo.class.equals(query.getType())) {
            Query<LayerInfo> lq = new Query<>(LayerInfo.class, query);
            Query<LayerGroupInfo> lgq = new Query<>(LayerGroupInfo.class, query);
            Stream<LayerInfo> layers = query(lq);
            Stream<LayerGroupInfo> groups = query(lgq);
            Comparator<CatalogInfo> comparator = CatalogInfoLookup.toComparator(query);
            stream = Stream.concat(layers, groups).sorted(comparator).map(query.getType()::cast);
        } else {
            try {
                checkCanSort(query);
                stream = repository(query.getType()).findAll(query);
            } catch (RuntimeException e) {
                throw new CatalogException("Error obtaining stream: %s".formatted(query), e);
            }
        }
        return stream;
    }

    @Override
    public <I extends CatalogInfo> I update(I info, Patch patch) {
        checkNotAProxy(info);
        CatalogInfoRepository<I> repo = repositoryFor(info);
        return repo.update(info, patch);
    }

    private static void checkNotAProxy(CatalogInfo value) {
        if (Proxy.isProxyClass(value.getClass())) {
            throw new IllegalArgumentException(
                    "Proxy values shall not be passed to CatalogInfoLookup");
        }
    }
}
