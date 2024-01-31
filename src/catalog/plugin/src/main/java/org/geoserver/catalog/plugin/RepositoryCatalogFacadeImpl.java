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
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
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

public class RepositoryCatalogFacadeImpl
        implements RepositoryCatalogFacade, CatalogInfoRepositoryHolder {

    protected final CatalogInfoRepositoryHolderImpl repositories;

    protected Catalog catalog;

    protected final CatalogCapabilities capabilities = new CatalogCapabilities();

    public RepositoryCatalogFacadeImpl() {
        repositories = new CatalogInfoRepositoryHolderImpl();
    }

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
        return add(store, StoreInfo.class, getStoreRepository());
    }

    @Override
    public void remove(StoreInfo store) {
        getStoreRepository().remove(store);
    }

    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return getStoreRepository().findById(id, clazz).orElse(null);
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {

        Optional<T> result;
        if (workspace == ANY_WORKSPACE || workspace == null) {
            result = getStoreRepository().findFirstByName(name, clazz);
        } else {
            result = getStoreRepository().findByNameAndWorkspace(name, workspace, clazz);
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

        return toList(() -> getStoreRepository().findAllByWorkspace(ws, clazz));
    }

    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return toList(() -> getStoreRepository().findAllByType(clazz));
    }

    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return getStoreRepository().getDefaultDataStore(workspace).orElse(null);
    }

    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        if (store != null) {
            Objects.requireNonNull(store.getWorkspace());
            Assert.isTrue(
                    workspace.getId().equals(store.getWorkspace().getId()),
                    "Store workspace mismatch");
        }

        if (store == null) getStoreRepository().unsetDefaultDataStore(workspace);
        else getStoreRepository().setDefaultDataStore(workspace, store);
    }

    //
    // Resources
    //
    @Override
    public ResourceInfo add(ResourceInfo resource) {
        return add(resource, ResourceInfo.class, getResourceRepository());
    }

    @Override
    public void remove(ResourceInfo resource) {
        getResourceRepository().remove(resource);
    }

    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return getResourceRepository().findById(id, clazz).orElse(null);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo namespace, String name, Class<T> clazz) {
        Optional<T> result;
        if (namespace == ANY_NAMESPACE) {
            result = getResourceRepository().findFirstByName(name, clazz);
        } else {
            result = getResourceRepository().findByNameAndNamespace(name, namespace, clazz);
        }

        return result.orElse(null);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return toList(() -> getResourceRepository().findAllByType(clazz));
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        // Question: do we need to support ANY_WORKSPACE? see "todo" comment in DefaultCatalogFacade
        NamespaceInfo ns = namespace == null ? getDefaultNamespace() : namespace;
        return toList(() -> getResourceRepository().findAllByNamespace(ns, clazz));
    }

    @Override
    public <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        Optional<T> resource;
        NamespaceInfo ns = null;
        if (store.getWorkspace() != null
                && store.getWorkspace().getName() != null
                && (ns = getNamespaceByPrefix(store.getWorkspace().getName())) != null) {

            resource = getResourceRepository().findByNameAndNamespace(name, ns, clazz);
            if (resource.isPresent() && !(store.equals(resource.get().getStore()))) {
                return null;
            }
        } else {
            // should not happen, but some broken test code sets up namespaces without equivalent
            // workspaces
            // or stores without workspaces
            resource = getResourceRepository().findByStoreAndName(store, name, clazz);
        }
        return resource.orElse(null);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return toList(() -> getResourceRepository().findAllByStore(store, clazz));
    }

    //
    // Layers
    //
    @Override
    public LayerInfo add(LayerInfo layer) {
        return add(layer, LayerInfo.class, getLayerRepository());
    }

    @Override
    public void remove(LayerInfo layer) {
        getLayerRepository().remove(layer);
    }

    @Override
    public LayerInfo getLayer(String id) {
        return getLayerRepository().findById(id, LayerInfo.class).orElse(null);
    }

    @Override
    public LayerInfo getLayerByName(String name) {
        return getLayerRepository().findOneByName(name).orElse(null);
    }

    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return toList(() -> getLayerRepository().findAllByResource(resource));
    }

    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {
        return toList(() -> getLayerRepository().findAllByDefaultStyleOrStyles(style));
    }

    @Override
    public List<LayerInfo> getLayers() {
        return toList(getLayerRepository()::findAll);
    }

    //
    // Maps
    //
    @Override
    public MapInfo add(MapInfo map) {
        return add(map, MapInfo.class, getMapRepository());
    }

    @Override
    public void remove(MapInfo map) {
        getMapRepository().remove(map);
    }

    @Override
    public MapInfo getMap(String id) {
        return getMapRepository().findById(id, MapInfo.class).orElse(null);
    }

    @Override
    public MapInfo getMapByName(String name) {
        return getMapRepository().findFirstByName(name, MapInfo.class).orElse(null);
    }

    @Override
    public List<MapInfo> getMaps() {
        return toList(getMapRepository()::findAll);
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
        return add(namespace, NamespaceInfo.class, getNamespaceRepository());
    }

    @Override
    public void remove(NamespaceInfo namespace) {
        NamespaceInfo defaultNamespace = getDefaultNamespace();
        if (defaultNamespace != null && namespace.getId().equals(defaultNamespace.getId())) {
            setDefaultNamespace(null);
        }
        getNamespaceRepository().remove(namespace);
    }

    @Override
    public NamespaceInfo getDefaultNamespace() {
        return getNamespaceRepository().getDefaultNamespace().orElse(null);
    }

    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamnespace) {
        if (defaultNamnespace == null) getNamespaceRepository().unsetDefaultNamespace();
        else getNamespaceRepository().setDefaultNamespace(defaultNamnespace);
    }

    @Override
    public NamespaceInfo getNamespace(String id) {
        return getNamespaceRepository().findById(id, NamespaceInfo.class).orElse(null);
    }

    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return getNamespaceRepository().findFirstByName(prefix, NamespaceInfo.class).orElse(null);
    }

    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        return getNamespaceRepository().findOneByURI(uri).orElse(null);
    }

    @Override
    public List<NamespaceInfo> getNamespacesByURI(String uri) {
        return toList(() -> getNamespaceRepository().findAllByURI(uri));
    }

    @Override
    public List<NamespaceInfo> getNamespaces() {
        return toList(getNamespaceRepository()::findAll);
    }

    //
    // Workspaces
    //
    // Workspace methods
    @Override
    public WorkspaceInfo add(WorkspaceInfo workspace) {
        return add(workspace, WorkspaceInfo.class, getWorkspaceRepository());
    }

    @Override
    public void remove(WorkspaceInfo workspace) {
        WorkspaceInfo defaultWorkspace = getDefaultWorkspace();
        if (defaultWorkspace != null && workspace.getId().equals(defaultWorkspace.getId())) {
            getWorkspaceRepository().unsetDefaultWorkspace();
        }
        getWorkspaceRepository().remove(workspace);
    }

    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return getWorkspaceRepository().getDefaultWorkspace().orElse(null);
    }

    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        WorkspaceInfo ws = workspace;
        if (ws == null) getWorkspaceRepository().unsetDefaultWorkspace();
        else getWorkspaceRepository().setDefaultWorkspace(ws);
    }

    @Override
    public List<WorkspaceInfo> getWorkspaces() {
        return toList(getWorkspaceRepository()::findAll);
    }

    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return getWorkspaceRepository().findById(id, WorkspaceInfo.class).orElse(null);
    }

    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        return getWorkspaceRepository().findFirstByName(name, WorkspaceInfo.class).orElse(null);
    }

    //
    // Styles
    //
    @Override
    public StyleInfo add(StyleInfo style) {
        return add(style, StyleInfo.class, getStyleRepository());
    }

    @Override
    public void remove(StyleInfo style) {
        getStyleRepository().remove(style);
    }

    @Override
    public StyleInfo getStyle(String id) {
        return getStyleRepository().findById(id, StyleInfo.class).orElse(null);
    }

    @Override
    public StyleInfo getStyleByName(String name) {
        Optional<StyleInfo> match = getStyleRepository().findByNameAndWordkspaceNull(name);
        if (match.isEmpty()) {
            match = getStyleRepository().findFirstByName(name, StyleInfo.class);
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
            match = getStyleRepository().findByNameAndWordkspaceNull(name);
        } else {
            match = getStyleRepository().findByNameAndWorkspace(name, workspace);
        }
        return match.orElse(null);
    }

    @Override
    public List<StyleInfo> getStyles() {
        return toList(getStyleRepository()::findAll);
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        // Question: do we need to support ANY_WORKSPACE? see "todo" comment in DefaultCatalogFacade
        Stream<StyleInfo> matches;
        if (workspace == NO_WORKSPACE) {
            matches = getStyleRepository().findAllByNullWorkspace();
        } else {
            WorkspaceInfo ws;
            if (workspace == null) {
                ws = getDefaultWorkspace();
            } else {
                ws = workspace;
            }

            matches = getStyleRepository().findAllByWorkspace(ws);
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
        repositories.dispose();
    }

    @Override
    public void syncTo(CatalogFacade to) {
        final CatalogFacade dao = ProxyUtils.unwrap(to, LockingCatalogFacade.class);
        if (dao instanceof CatalogInfoRepositoryHolder other) {
            // do an optimized sync
            this.getWorkspaceRepository().syncTo(other.getWorkspaceRepository());
            this.getNamespaceRepository().syncTo(other.getNamespaceRepository());
            this.getStoreRepository().syncTo(other.getStoreRepository());
            this.getResourceRepository().syncTo(other.getResourceRepository());
            this.getLayerRepository().syncTo(other.getLayerRepository());
            this.getLayerGroupRepository().syncTo(other.getLayerGroupRepository());
            this.getStyleRepository().syncTo(other.getStyleRepository());
            this.getMapRepository().syncTo(other.getMapRepository());
            dao.setCatalog(catalog);
        } else {
            // do a manual import
            sync(getWorkspaceRepository()::findAll, dao::add);
            sync(getNamespaceRepository()::findAll, dao::add);
            sync(getStoreRepository()::findAll, dao::add);
            sync(getResourceRepository()::findAll, dao::add);
            sync(getStyleRepository()::findAll, dao::add);
            sync(getLayerRepository()::findAll, dao::add);
            sync(getLayerGroupRepository()::findAll, dao::add);
            sync(getMapRepository()::findAll, dao::add);
        }

        dao.setDefaultWorkspace(getDefaultWorkspace());
        dao.setDefaultNamespace(getDefaultNamespace());
        try (Stream<DataStoreInfo> defaultDataStores =
                getStoreRepository().getDefaultDataStores()) {
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

    @Override
    public <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repository(Class<T> of) {
        return repositories.repository(of);
    }

    @Override
    public <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repositoryFor(T info) {
        return repositories.repositoryFor(info);
    }

    @Override
    public void setNamespaceRepository(NamespaceRepository namespaces) {
        repositories.setNamespaceRepository(namespaces);
    }

    @Override
    public NamespaceRepository getNamespaceRepository() {
        return repositories.getNamespaceRepository();
    }

    @Override
    public void setWorkspaceRepository(WorkspaceRepository workspaces) {
        repositories.setWorkspaceRepository(workspaces);
    }

    @Override
    public WorkspaceRepository getWorkspaceRepository() {
        return repositories.getWorkspaceRepository();
    }

    @Override
    public void setStoreRepository(StoreRepository stores) {
        repositories.setStoreRepository(stores);
    }

    @Override
    public StoreRepository getStoreRepository() {
        return repositories.getStoreRepository();
    }

    @Override
    public void setResourceRepository(ResourceRepository resources) {
        repositories.setResourceRepository(resources);
    }

    @Override
    public ResourceRepository getResourceRepository() {
        return repositories.getResourceRepository();
    }

    @Override
    public void setLayerRepository(LayerRepository layers) {
        repositories.setLayerRepository(layers);
    }

    @Override
    public LayerRepository getLayerRepository() {
        return repositories.getLayerRepository();
    }

    @Override
    public void setLayerGroupRepository(LayerGroupRepository layerGroups) {
        repositories.setLayerGroupRepository(layerGroups);
    }

    @Override
    public LayerGroupRepository getLayerGroupRepository() {
        return repositories.getLayerGroupRepository();
    }

    @Override
    public void setStyleRepository(StyleRepository styles) {
        repositories.setStyleRepository(styles);
    }

    @Override
    public StyleRepository getStyleRepository() {
        return repositories.getStyleRepository();
    }

    @Override
    public void setMapRepository(MapRepository maps) {
        repositories.setMapRepository(maps);
    }

    @Override
    public MapRepository getMapRepository() {
        return repositories.getMapRepository();
    }
}
