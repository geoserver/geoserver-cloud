/*
 * (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans This
 * code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static java.lang.String.format;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogCapabilities;
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
import org.geotools.util.logging.Logging;
import org.springframework.util.Assert;

import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RepositoryCatalogFacadeImpl extends CatalogInfoRepositoryHolderImpl
        implements RepositoryCatalogFacade {

    private static final Logger LOGGER = Logging.getLogger(RepositoryCatalogFacadeImpl.class);

    protected Catalog catalog;

    protected final CatalogCapabilities capabilities = new CatalogCapabilities();

    public RepositoryCatalogFacadeImpl() {}

    public RepositoryCatalogFacadeImpl(Catalog catalog) {
        this();
        setCatalog(catalog);
    }

    public @Override CatalogCapabilities getCatalogCapabilities() {
        return capabilities;
    }

    public @Override void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    public @Override Catalog getCatalog() {
        return catalog;
    }

    public @Override void resolve() {
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
    public @Override StoreInfo add(StoreInfo store) {
        return add(store, StoreInfo.class, stores);
    }

    public @Override void remove(StoreInfo store) {
        stores.remove(store);
    }

    public @Override <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return stores.findById(id, clazz).orElse(null);
    }

    public @Override <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {

        Optional<T> result;
        if (workspace == ANY_WORKSPACE || workspace == null) {
            result = stores.findFirstByName(name, clazz);
        } else {
            result = stores.findByNameAndWorkspace(name, workspace, clazz);
        }
        return result.orElse(null);
    }

    public @Override <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        // TODO: support ANY_WORKSPACE?
        final WorkspaceInfo ws;
        if (workspace == null) {
            ws = getDefaultWorkspace();
        } else {
            ws = workspace;
        }

        return toList(() -> stores.findAllByWorkspace(ws, clazz));
    }

    public @Override <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return toList(() -> stores.findAllByType(clazz));
    }

    public @Override DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return stores.getDefaultDataStore(workspace).orElse(null);
    }

    public @Override void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
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
    public @Override ResourceInfo add(ResourceInfo resource) {
        return add(resource, ResourceInfo.class, resources);
    }

    public @Override void remove(ResourceInfo resource) {
        resources.remove(resource);
    }

    public @Override <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return resources.findById(id, clazz).orElse(null);
    }

    public @Override <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo namespace, String name, Class<T> clazz) {
        Optional<T> result;
        if (namespace == ANY_NAMESPACE) {
            result = resources.findFirstByName(name, clazz);
        } else {
            result = resources.findByNameAndNamespace(name, namespace, clazz);
        }

        return result.orElse(null);
    }

    public @Override <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return toList(() -> resources.findAllByType(clazz));
    }

    public @Override <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        // TODO: support ANY_NAMESPACE?
        NamespaceInfo ns = namespace == null ? getDefaultNamespace() : namespace;
        return toList(() -> resources.findAllByNamespace(ns, clazz));
    }

    public @Override <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        Optional<T> resource = null;
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

    public @Override <T extends ResourceInfo> List<T> getResourcesByStore(
            StoreInfo store, Class<T> clazz) {
        return toList(() -> resources.findAllByStore(store, clazz));
    }

    //
    // Layers
    //
    public @Override LayerInfo add(LayerInfo layer) {
        return add(layer, LayerInfo.class, layers);
    }

    public @Override void remove(LayerInfo layer) {
        layers.remove(layer);
    }

    public @Override LayerInfo getLayer(String id) {
        return layers.findById(id, LayerInfo.class).orElse(null);
    }

    public @Override LayerInfo getLayerByName(String name) {
        return layers.findOneByName(name).orElse(null);
    }

    public @Override List<LayerInfo> getLayers(ResourceInfo resource) {
        return toList(() -> layers.findAllByResource(resource));
    }

    public @Override List<LayerInfo> getLayers(StyleInfo style) {
        return toList(() -> layers.findAllByDefaultStyleOrStyles(style));
    }

    public @Override List<LayerInfo> getLayers() {
        return toList(layers::findAll);
    }

    //
    // Maps
    //
    public @Override MapInfo add(MapInfo map) {
        return add(map, MapInfo.class, maps);
    }

    public @Override void remove(MapInfo map) {
        maps.remove(map);
    }

    public @Override MapInfo getMap(String id) {
        return maps.findById(id, MapInfo.class).orElse(null);
    }

    public @Override MapInfo getMapByName(String name) {
        return maps.findFirstByName(name, MapInfo.class).orElse(null);
    }

    public @Override List<MapInfo> getMaps() {
        return toList(maps::findAll);
    }

    //
    // Layer groups
    //
    public @Override LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return add(layerGroup, LayerGroupInfo.class, getLayerGroupRepository());
    }

    public @Override void remove(LayerGroupInfo layerGroup) {
        getLayerGroupRepository().remove(layerGroup);
    }

    public @Override List<LayerGroupInfo> getLayerGroups() {
        return toList(getLayerGroupRepository()::findAll);
    }

    public @Override List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        // TODO: support ANY_WORKSPACE?

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

    public @Override LayerGroupInfo getLayerGroup(String id) {
        return getLayerGroupRepository().findById(id, LayerGroupInfo.class).orElse(null);
    }

    public @Override LayerGroupInfo getLayerGroupByName(String name) {
        return getLayerGroupByName(NO_WORKSPACE, name);
    }

    public @Override LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {

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
    public @Override NamespaceInfo add(NamespaceInfo namespace) {
        return add(namespace, NamespaceInfo.class, namespaces);
    }

    public @Override void remove(NamespaceInfo namespace) {
        NamespaceInfo defaultNamespace = getDefaultNamespace();
        if (defaultNamespace != null && namespace.getId().equals(defaultNamespace.getId())) {
            setDefaultNamespace(null);
        }
        namespaces.remove(namespace);
    }

    public @Override NamespaceInfo getDefaultNamespace() {
        return namespaces.getDefaultNamespace().orElse(null);
    }

    public @Override void setDefaultNamespace(NamespaceInfo defaultNamnespace) {
        if (defaultNamnespace == null) namespaces.unsetDefaultNamespace();
        else namespaces.setDefaultNamespace(defaultNamnespace);
    }

    public @Override NamespaceInfo getNamespace(String id) {
        return namespaces.findById(id, NamespaceInfo.class).orElse(null);
    }

    public @Override NamespaceInfo getNamespaceByPrefix(String prefix) {
        return namespaces.findFirstByName(prefix, NamespaceInfo.class).orElse(null);
    }

    public @Override NamespaceInfo getNamespaceByURI(String uri) {
        return namespaces.findOneByURI(uri).orElse(null);
    }

    public @Override List<NamespaceInfo> getNamespacesByURI(String uri) {
        return toList(() -> namespaces.findAllByURI(uri));
    }

    public @Override List<NamespaceInfo> getNamespaces() {
        return toList(namespaces::findAll);
    }

    //
    // Workspaces
    //
    // Workspace methods
    public @Override WorkspaceInfo add(WorkspaceInfo workspace) {
        return add(workspace, WorkspaceInfo.class, workspaces);
    }

    public @Override void remove(WorkspaceInfo workspace) {
        WorkspaceInfo defaultWorkspace = getDefaultWorkspace();
        if (defaultWorkspace != null && workspace.getId().equals(defaultWorkspace.getId())) {
            workspaces.unsetDefaultWorkspace();
        }
        workspaces.remove(workspace);
    }

    public @Override WorkspaceInfo getDefaultWorkspace() {
        return workspaces.getDefaultWorkspace().orElse(null);
    }

    public @Override void setDefaultWorkspace(WorkspaceInfo workspace) {
        WorkspaceInfo ws = workspace;
        if (ws == null) workspaces.unsetDefaultWorkspace();
        else workspaces.setDefaultWorkspace(ws);
    }

    public @Override List<WorkspaceInfo> getWorkspaces() {
        return toList(workspaces::findAll);
    }

    public @Override WorkspaceInfo getWorkspace(String id) {
        return workspaces.findById(id, WorkspaceInfo.class).orElse(null);
    }

    public @Override WorkspaceInfo getWorkspaceByName(String name) {
        return workspaces.findFirstByName(name, WorkspaceInfo.class).orElse(null);
    }

    //
    // Styles
    //
    public @Override StyleInfo add(StyleInfo style) {
        return add(style, StyleInfo.class, styles);
    }

    public @Override void remove(StyleInfo style) {
        styles.remove(style);
    }

    public @Override StyleInfo getStyle(String id) {
        return styles.findById(id, StyleInfo.class).orElse(null);
    }

    public @Override StyleInfo getStyleByName(String name) {
        Optional<StyleInfo> match = styles.findByNameAndWordkspaceNull(name);
        if (match.isEmpty()) {
            match = styles.findFirstByName(name, StyleInfo.class);
        }
        return match.orElse(null);
    }

    public @Override StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
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

    public @Override List<StyleInfo> getStyles() {
        return toList(styles::findAll);
    }

    public @Override List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        // TODO: support ANY_WORKSPACE?
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
            return stream.collect(Collectors.toList());
        }
    }

    public @Override void dispose() {
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

    public @Override void syncTo(CatalogFacade to) {
        final CatalogFacade dao = ProxyUtils.unwrap(to, LockingCatalogFacade.class);
        if (dao instanceof CatalogInfoRepositoryHolder) {
            // do an optimized sync
            CatalogInfoRepositoryHolder other = (CatalogInfoRepositoryHolder) dao;
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

    public @Override <T extends CatalogInfo> int count(final Class<T> of, final Filter filter) {
        long count;
        if (PublishedInfo.class.equals(of)) {
            long layers = count(LayerInfo.class, filter);
            long groups = count(LayerGroupInfo.class, filter);
            count = layers + groups;
        } else {
            try {
                count = repository(of).count(of, filter);
            } catch (RuntimeException e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error obtaining count of "
                                + of.getSimpleName()
                                + " with filter "
                                + filter);
                throw e;
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
    public @Override boolean canSort(
            final Class<? extends CatalogInfo> type, final String propertyName) {
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

    public @Override <T extends CatalogInfo> Stream<T> query(Query<T> query) {
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
                LOGGER.log(Level.SEVERE, "Error obtaining stream: " + query, e);
                throw e;
            }
        }
        return stream;
    }

    public @Override <I extends CatalogInfo> I update(I info, Patch patch) {
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
