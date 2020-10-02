/*
 * (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans This
 * code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static java.lang.String.format;
import static org.geoserver.catalog.impl.ClassMappings.LAYER;
import static org.geoserver.catalog.impl.ClassMappings.LAYERGROUP;
import static org.geoserver.catalog.impl.ClassMappings.MAP;
import static org.geoserver.catalog.impl.ClassMappings.NAMESPACE;
import static org.geoserver.catalog.impl.ClassMappings.RESOURCE;
import static org.geoserver.catalog.impl.ClassMappings.STORE;
import static org.geoserver.catalog.impl.ClassMappings.STYLE;
import static org.geoserver.catalog.impl.ClassMappings.WORKSPACE;

import java.lang.reflect.Proxy;
import java.rmi.server.UID;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogCapabilities;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Info;
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
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ProxyUtils;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.springframework.util.Assert;

public class RepositoryCatalogFacadeImpl implements RepositoryCatalogFacade {

    private static final Logger LOGGER = Logging.getLogger(RepositoryCatalogFacadeImpl.class);

    protected NamespaceRepository namespaces;
    protected WorkspaceRepository workspaces;
    protected StoreRepository stores;
    protected ResourceRepository resources;
    protected LayerRepository layers;
    protected LayerGroupRepository layerGroups;
    protected MapRepository maps;
    protected StyleRepository styles;
    protected Catalog catalog;

    private final EnumMap<ClassMappings, Supplier<CatalogInfoRepository<?>>> repos;

    protected final CatalogCapabilities capabilities = new CatalogCapabilities();

    private void registerRepository(ClassMappings cm, Supplier<CatalogInfoRepository<?>> repo) {
        repos.put(cm, repo);
        for (Class<? extends Info> c : cm.concreteInterfaces()) {
            ClassMappings i = ClassMappings.fromInterface(c);
            if (!cm.getInterface().equals(i.getInterface())) repos.put(i, repo);
        }
    }

    public RepositoryCatalogFacadeImpl() {
        repos = new EnumMap<>(ClassMappings.class);
        registerRepository(WORKSPACE, () -> workspaces);
        registerRepository(NAMESPACE, () -> namespaces);
        registerRepository(STORE, () -> stores);
        registerRepository(RESOURCE, () -> resources);
        registerRepository(LAYER, () -> layers);
        registerRepository(LAYERGROUP, () -> layerGroups);
        registerRepository(STYLE, () -> styles);
        registerRepository(MAP, () -> maps);
    }

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

    public void setNamespaceRepository(NamespaceRepository namespaces) {
        this.namespaces = namespaces;
    }

    public void setWorkspaceRepository(WorkspaceRepository workspaces) {
        this.workspaces = workspaces;
    }

    public void setStoreRepository(StoreRepository stores) {
        this.stores = stores;
    }

    public void setResourceRepository(ResourceRepository resources) {
        this.resources = resources;
    }

    public void setLayerRepository(LayerRepository layers) {
        this.layers = layers;
    }

    public void setLayerGroupRepository(LayerGroupRepository layerGroups) {
        this.layerGroups = layerGroups;
    }

    public void setStyleRepository(StyleRepository styles) {
        this.styles = styles;
    }

    public void setMapRepository(MapRepository maps) {
        this.maps = maps;
    }

    public @Override NamespaceRepository getNamespaceRepository() {
        return namespaces;
    }

    public @Override WorkspaceRepository getWorkspaceRepository() {
        return workspaces;
    }

    public @Override StoreRepository getStoreRepository() {
        return stores;
    }

    public @Override ResourceRepository getResourceRepository() {
        return resources;
    }

    public @Override LayerRepository getLayerRepository() {
        return layers;
    }

    public @Override LayerGroupRepository getLayerGroupRepository() {
        return layerGroups;
    }

    public @Override StyleRepository getStyleRepository() {
        return styles;
    }

    public @Override MapRepository getMapRepository() {
        return maps;
    }

    public @Override void resolve() {
        // no-op, override as appropriate
    };

    protected <I extends CatalogInfo> I add(
            I info, Class<I> type, CatalogInfoRepository<I> repository) {
        checkNotAProxy(info);
        setId(info);
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

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(StoreInfo store) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override <T extends StoreInfo> T detach(T store) {
        return store;
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
        DataStoreInfo old = stores.getDefaultDataStore(workspace).orElse(null);
        if (store != null) {
            Objects.requireNonNull(store.getWorkspace());
            Assert.isTrue(
                    workspace.getId().equals(store.getWorkspace().getId()),
                    "Store workspace mismatch");
        }

        // fire modify event before change
        catalog.fireModified(catalog, asList("defaultDataStore"), asList(old), asList(store));

        if (store == null) stores.unsetDefaultDataStore(workspace);
        else stores.setDefaultDataStore(workspace, store);

        // fire postmodify event after change
        catalog.firePostModified(catalog, asList("defaultDataStore"), asList(old), asList(store));
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

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(ResourceInfo resource) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override <T extends ResourceInfo> T detach(T resource) {
        return resource;
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

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(LayerInfo layer) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override LayerInfo detach(LayerInfo layer) {
        return layer;
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

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(MapInfo map) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override MapInfo detach(MapInfo map) {
        return map;
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
        return add(layerGroup, LayerGroupInfo.class, layerGroups);
    }

    public @Override void remove(LayerGroupInfo layerGroup) {
        layerGroups.remove(layerGroup);
    }

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(LayerGroupInfo layerGroup) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return layerGroup;
    }

    public @Override List<LayerGroupInfo> getLayerGroups() {
        return toList(layerGroups::findAll);
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
            matches = layerGroups.findAllByWorkspaceIsNull();
        } else {
            matches = layerGroups.findAllByWorkspace(ws);
        }
        return toList(() -> matches);
    }

    public @Override LayerGroupInfo getLayerGroup(String id) {
        return layerGroups.findById(id, LayerGroupInfo.class).orElse(null);
    }

    public @Override LayerGroupInfo getLayerGroupByName(String name) {
        return getLayerGroupByName(NO_WORKSPACE, name);
    }

    public @Override LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {

        if (workspace == NO_WORKSPACE)
            return layerGroups.findByNameAndWorkspaceIsNull(name).orElse(null);

        if (ANY_WORKSPACE == workspace)
            return layerGroups.findFirstByName(name, LayerGroupInfo.class).orElse(null);

        return layerGroups.findByNameAndWorkspace(name, workspace).orElse(null);
    }

    //
    // Namespaces
    //
    public @Override NamespaceInfo add(NamespaceInfo namespace) {
        return add(namespace, NamespaceInfo.class, namespaces);
    }

    public @Override void remove(NamespaceInfo namespace) {
        if (namespace.equals(getDefaultNamespace())) {
            setDefaultNamespace(null);
        }
        namespaces.remove(namespace);
    }

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(NamespaceInfo namespace) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override NamespaceInfo detach(NamespaceInfo namespace) {
        return namespace;
    }

    public @Override NamespaceInfo getDefaultNamespace() {
        return namespaces.getDefaultNamespace().orElse(null);
    }

    public @Override void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        NamespaceInfo old = getDefaultNamespace();
        // fire modify event before change
        catalog.fireModified(
                catalog, asList("defaultNamespace"), asList(old), asList(defaultNamespace));

        NamespaceInfo ns = defaultNamespace;
        if (ns == null) namespaces.unsetDefaultNamespace();
        else namespaces.setDefaultNamespace(ns);

        // fire postmodify event after change
        catalog.firePostModified(
                catalog, asList("defaultNamespace"), asList(old), asList(defaultNamespace));
    }

    // if value is null, the list is a singleton list with a null member
    private <T> List<T> asList(@Nullable T value) {
        return Collections.singletonList(value);
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
        if (workspace.equals(getDefaultWorkspace())) {
            workspaces.unsetDefaultWorkspace();
        }
        workspaces.remove(workspace);
    }

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(WorkspaceInfo workspace) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override WorkspaceInfo detach(WorkspaceInfo workspace) {
        return workspace;
    }

    public @Override WorkspaceInfo getDefaultWorkspace() {
        return workspaces.getDefaultWorkspace().orElse(null);
    }

    public @Override void setDefaultWorkspace(WorkspaceInfo workspace) {
        WorkspaceInfo old = getDefaultWorkspace();
        // fire modify event before change
        catalog.fireModified(catalog, asList("defaultWorkspace"), asList(old), asList(workspace));

        WorkspaceInfo ws = workspace;
        if (ws == null) workspaces.unsetDefaultWorkspace();
        else workspaces.setDefaultWorkspace(ws);

        // fire postmodify event after change
        catalog.firePostModified(
                catalog, asList("defaultWorkspace"), asList(old), asList(workspace));
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

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(StyleInfo style) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override StyleInfo detach(StyleInfo style) {
        return style;
    }

    public @Override StyleInfo getStyle(String id) {
        return styles.findById(id, StyleInfo.class).orElse(null);
    }

    public @Override StyleInfo getStyleByName(String name) {
        Optional<StyleInfo> match = styles.findByNameAndWordkspaceNull(name);
        if (match == null) {
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
            match = styles.findByNameAndWordkspace(name, workspace);
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
        try (Stream<T> matches = query(Query.valueOf(of, filter))) {
            long count = matches.count();
            return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
        }
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

    @SuppressWarnings("unchecked")
    protected <I extends CatalogInfo> I resolve(I info) {
        if (info instanceof LayerGroupInfo) {
            return (I) resolve((LayerGroupInfo) info);
        } else if (info instanceof LayerInfo) {
            return (I) resolve((LayerInfo) info);
        } else if (info instanceof MapInfo) {
            return (I) resolve((MapInfo) info);
        } else if (info instanceof NamespaceInfo) {
            return (I) resolve((NamespaceInfo) info);
        } else if (info instanceof ResourceInfo) {
            return (I) resolve((ResourceInfo) info);
        } else if (info instanceof StoreInfo) {
            return (I) resolve((StoreInfo) info);
        } else if (info instanceof StyleInfo) {
            return (I) resolve((StyleInfo) info);
        } else if (info instanceof WorkspaceInfo) {
            return (I) resolve((WorkspaceInfo) info);
        }
        throw new IllegalArgumentException("Unknown resource type: " + info);
    }

    protected void setId(CatalogInfo o) {
        if (OwsUtils.get(o, "id") == null) {
            String uid = new UID().toString();
            OwsUtils.set(o, "id", o.getClass().getSimpleName() + "-" + uid);
        }
    }

    public @Override <I extends CatalogInfo> I update(I info, Patch patch) {
        checkNotAProxy(info);
        CatalogInfoRepository<I> repo = repository(info.getClass());
        return repo.update(info, patch);
    }

    @SuppressWarnings("unchecked")
    protected <I extends CatalogInfo, R extends CatalogInfoRepository<I>> R repository(
            Class<? extends CatalogInfo> of) {
        ClassMappings cm = ClassMappings.fromImpl(of);
        if (cm == null) cm = ClassMappings.fromInterface(of);
        R repo = (R) repos.get(cm).get();
        if (repo == null) throw new IllegalArgumentException("Unknown type: " + of);
        return repo;
    }

    private static void checkNotAProxy(CatalogInfo value) {
        if (Proxy.isProxyClass(value.getClass())) {
            throw new IllegalArgumentException(
                    "Proxy values shall not be passed to CatalogInfoLookup");
        }
    }
}
