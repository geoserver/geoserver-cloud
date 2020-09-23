/*
 * (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans This
 * code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static org.geoserver.catalog.impl.ClassMappings.LAYER;
import static org.geoserver.catalog.impl.ClassMappings.LAYERGROUP;
import static org.geoserver.catalog.impl.ClassMappings.MAP;
import static org.geoserver.catalog.impl.ClassMappings.NAMESPACE;
import static org.geoserver.catalog.impl.ClassMappings.RESOURCE;
import static org.geoserver.catalog.impl.ClassMappings.STORE;
import static org.geoserver.catalog.impl.ClassMappings.STYLE;
import static org.geoserver.catalog.impl.ClassMappings.WORKSPACE;

import com.google.common.collect.Ordering;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.server.UID;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.experimental.Accessors;
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
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ProxyUtils;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.springframework.util.Assert;

@Accessors(fluent = true)
public abstract class AbstractCatalogFacade implements CatalogFacade {

    private static final Logger LOGGER = Logging.getLogger(AbstractCatalogFacade.class);

    protected @Getter NamespaceRepository namespaces;
    protected @Getter WorkspaceRepository workspaces;
    protected @Getter StoreRepository stores;
    protected @Getter ResourceRepository resources;
    protected @Getter LayerRepository layers;
    protected @Getter LayerGroupRepository layerGroups;
    protected @Getter MapRepository maps;
    protected @Getter StyleRepository styles;
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

    public AbstractCatalogFacade() {
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

    public AbstractCatalogFacade(Catalog catalog) {
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

    public void setNamespaces(NamespaceRepository namespaces) {
        this.namespaces = namespaces;
    }

    public void setWorkspaces(WorkspaceRepository workspaces) {
        this.workspaces = workspaces;
    }

    public void setStores(StoreRepository stores) {
        this.stores = stores;
    }

    public void setResources(ResourceRepository resources) {
        this.resources = resources;
    }

    public void setLayers(LayerRepository layers) {
        this.layers = layers;
    }

    public void setLayerGroups(LayerGroupRepository layerGroups) {
        this.layerGroups = layerGroups;
    }

    public void setStyles(StyleRepository styles) {
        this.styles = styles;
    }

    public void setMaps(MapRepository maps) {
        this.maps = maps;
    }

    public @Override abstract void resolve();

    protected <I extends CatalogInfo> I add(
            I info, Class<I> type, CatalogInfoRepository<I> repository) {
        info = unwrap(info);
        setId(info);
        info = resolve(info);
        repository.add(info);
        return verifyBeforeReturning(info, type);
    }

    //
    // Stores
    //
    public @Override StoreInfo add(StoreInfo store) {
        return add(store, StoreInfo.class, stores);
    }

    public @Override void remove(StoreInfo store) {
        stores.remove(unwrap(store));
    }

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(StoreInfo store) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override <T extends StoreInfo> T detach(T store) {
        return store;
    }

    public @Override <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return verifyBeforeReturning(stores.findById(id, clazz), clazz);
    }

    public @Override <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {

        T result;
        if (workspace == ANY_WORKSPACE || workspace == null) {
            result = stores.findFirstByName(name, clazz);
        } else {
            result = stores.findByNameAndWorkspace(name, workspace, clazz);
        }

        return verifyBeforeReturning(result, clazz);
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

        List<T> matches = toList(() -> stores.findAllByWorkspace(ws, clazz));
        return verifyBeforeReturning(matches, clazz);
    }

    public @Override <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return verifyBeforeReturning(toList(() -> stores.findAllByType(clazz)), clazz);
    }

    public @Override DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return verifyBeforeReturning(stores.getDefaultDataStore(workspace), DataStoreInfo.class);
    }

    public @Override void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        DataStoreInfo old = stores.getDefaultDataStore(workspace);
        if (store != null) {
            Objects.requireNonNull(store.getWorkspace());
            Assert.isTrue(
                    workspace.getId().equals(store.getWorkspace().getId()),
                    "Store workspace mismatch");
        }

        // fire modify event before change
        catalog.fireModified(catalog, asList("defaultDataStore"), asList(old), asList(store));

        stores.setDefaultDataStore(workspace, store);

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
        resources.remove(unwrap(resource));
    }

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(ResourceInfo resource) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override <T extends ResourceInfo> T detach(T resource) {
        return resource;
    }

    public @Override <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        T result = resources.findById(id, clazz);
        return verifyBeforeReturning(result, clazz);
    }

    public @Override <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo namespace, String name, Class<T> clazz) {
        T result;
        if (namespace == ANY_NAMESPACE) {
            result = resources.findFirstByName(name, clazz);
        } else {
            result = resources.findByNameAndNamespace(name, namespace, clazz);
        }

        return verifyBeforeReturning(result, clazz);
    }

    public @Override <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return verifyBeforeReturning(toList(() -> resources.findAllByType(clazz)), clazz);
    }

    public @Override <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        // TODO: support ANY_NAMESPACE?
        NamespaceInfo ns = namespace == null ? getDefaultNamespace() : namespace;
        List<T> matches = toList(() -> resources.findAllByNamespace(ns, clazz));
        return verifyBeforeReturning(matches, clazz);
    }

    public @Override <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        T resource = null;
        NamespaceInfo ns = null;
        if (store.getWorkspace() != null
                && store.getWorkspace().getName() != null
                && (ns = getNamespaceByPrefix(store.getWorkspace().getName())) != null) {

            resource = resources.findByNameAndNamespace(name, ns, clazz);
            if (resource != null && !(store.equals(resource.getStore()))) {
                return null;
            }
        } else {
            // should not happen, but some broken test code sets up namespaces without equivalent
            // workspaces
            // or stores without workspaces
            resource = resources.findByStoreAndName(store, name, clazz);
        }
        return verifyBeforeReturning(resource, clazz);
    }

    public @Override <T extends ResourceInfo> List<T> getResourcesByStore(
            StoreInfo store, Class<T> clazz) {
        List<T> matches = toList(() -> resources.findAllByStore(store, clazz));
        return verifyBeforeReturning(matches, clazz);
    }

    //
    // Layers
    //
    public @Override LayerInfo add(LayerInfo layer) {
        return add(layer, LayerInfo.class, layers);
    }

    public @Override void remove(LayerInfo layer) {
        layers.remove(unwrap(layer));
    }

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(LayerInfo layer) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override LayerInfo detach(LayerInfo layer) {
        return layer;
    }

    public @Override LayerInfo getLayer(String id) {
        LayerInfo li = layers.findById(id, LayerInfo.class);
        return verifyBeforeReturning(li, LayerInfo.class);
    }

    public @Override LayerInfo getLayerByName(String name) {
        LayerInfo result = layers.findOneByName(name);
        return verifyBeforeReturning(result, LayerInfo.class);
    }

    public @Override List<LayerInfo> getLayers(ResourceInfo resource) {
        List<LayerInfo> matches = toList(() -> layers.findAllByResource(resource));
        return verifyBeforeReturning(matches, LayerInfo.class);
    }

    public @Override List<LayerInfo> getLayers(StyleInfo style) {
        List<LayerInfo> matches = toList(() -> layers.findAllByDefaultStyleOrStyles(style));
        return verifyBeforeReturning(matches, LayerInfo.class);
    }

    public @Override List<LayerInfo> getLayers() {
        return verifyBeforeReturning(toList(layers::findAll), LayerInfo.class);
    }

    //
    // Maps
    //
    public @Override MapInfo add(MapInfo map) {
        return add(map, MapInfo.class, maps);
    }

    public @Override void remove(MapInfo map) {
        maps.remove(unwrap(map));
    }

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(MapInfo map) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override MapInfo detach(MapInfo map) {
        return map;
    }

    public @Override MapInfo getMap(String id) {
        return verifyBeforeReturning(maps.findById(id, MapInfo.class), MapInfo.class);
    }

    public @Override MapInfo getMapByName(String name) {
        return verifyBeforeReturning(maps.findFirstByName(name, MapInfo.class), MapInfo.class);
    }

    public @Override List<MapInfo> getMaps() {
        return verifyBeforeReturning(toList(maps::findAll), MapInfo.class);
    }

    //
    // Layer groups
    //
    public @Override LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return add(layerGroup, LayerGroupInfo.class, layerGroups);
    }

    public @Override void remove(LayerGroupInfo layerGroup) {
        layerGroups.remove(unwrap(layerGroup));
    }

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(LayerGroupInfo layerGroup) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return layerGroup;
    }

    public @Override List<LayerGroupInfo> getLayerGroups() {
        return verifyBeforeReturning(toList(layerGroups::findAll), LayerGroupInfo.class);
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
        return verifyBeforeReturning(toList(() -> matches), LayerGroupInfo.class);
    }

    public @Override LayerGroupInfo getLayerGroup(String id) {
        LayerGroupInfo result = layerGroups.findById(id, LayerGroupInfo.class);
        return verifyBeforeReturning(result, LayerGroupInfo.class);
    }

    public @Override LayerGroupInfo getLayerGroupByName(String name) {
        return getLayerGroupByName(NO_WORKSPACE, name);
    }

    public @Override LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        LayerGroupInfo match;
        if (workspace == NO_WORKSPACE) {
            match = layerGroups.findByNameAndWorkspaceIsNull(name);
        } else if (ANY_WORKSPACE == workspace) {
            match = layerGroups.findFirstByName(name, LayerGroupInfo.class);
        } else {
            match = layerGroups.findByNameAndWorkspace(name, workspace);
        }
        return verifyBeforeReturning(match, LayerGroupInfo.class);
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
        namespaces.remove(unwrap(namespace));
    }

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(NamespaceInfo namespace) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override NamespaceInfo detach(NamespaceInfo namespace) {
        return namespace;
    }

    public @Override NamespaceInfo getDefaultNamespace() {
        return verifyBeforeReturning(namespaces.getDefaultNamespace(), NamespaceInfo.class);
    }

    public @Override void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        NamespaceInfo old = getDefaultNamespace();
        // fire modify event before change
        catalog.fireModified(
                catalog, asList("defaultNamespace"), asList(old), asList(defaultNamespace));

        namespaces.setDefaultNamespace(unwrap(defaultNamespace));

        // fire postmodify event after change
        catalog.firePostModified(
                catalog, asList("defaultNamespace"), asList(old), asList(defaultNamespace));
    }

    // if value is null, the list is a singleton list with a null member
    private <T> List<T> asList(@Nullable T value) {
        return Collections.singletonList(value);
    }

    public @Override NamespaceInfo getNamespace(String id) {
        NamespaceInfo ns = namespaces.findById(id, NamespaceInfo.class);
        return verifyBeforeReturning(ns, NamespaceInfo.class);
    }

    public @Override NamespaceInfo getNamespaceByPrefix(String prefix) {
        NamespaceInfo ns = namespaces.findFirstByName(prefix, NamespaceInfo.class);
        return verifyBeforeReturning(ns, NamespaceInfo.class);
    }

    public @Override NamespaceInfo getNamespaceByURI(String uri) {
        return verifyBeforeReturning(namespaces.findOneByURI(uri), NamespaceInfo.class);
    }

    public @Override List<NamespaceInfo> getNamespacesByURI(String uri) {
        return verifyBeforeReturning(
                toList(() -> namespaces.findAllByURI(uri)), NamespaceInfo.class);
    }

    public @Override List<NamespaceInfo> getNamespaces() {
        return verifyBeforeReturning(toList(namespaces::findAll), NamespaceInfo.class);
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
            workspaces.setDefaultWorkspace(null);
        }
        workspaces.remove(unwrap(workspace));
    }

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(WorkspaceInfo workspace) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override WorkspaceInfo detach(WorkspaceInfo workspace) {
        return workspace;
    }

    public @Override WorkspaceInfo getDefaultWorkspace() {
        return verifyBeforeReturning(workspaces.getDefaultWorkspace(), WorkspaceInfo.class);
    }

    public @Override void setDefaultWorkspace(WorkspaceInfo workspace) {
        WorkspaceInfo old = getDefaultWorkspace();
        // fire modify event before change
        catalog.fireModified(catalog, asList("defaultWorkspace"), asList(old), asList(workspace));

        workspaces.setDefaultWorkspace(unwrap(workspace));

        // fire postmodify event after change
        catalog.firePostModified(
                catalog, asList("defaultWorkspace"), asList(old), asList(workspace));
    }

    public @Override List<WorkspaceInfo> getWorkspaces() {
        return verifyBeforeReturning(toList(workspaces::findAll), WorkspaceInfo.class);
    }

    public @Override WorkspaceInfo getWorkspace(String id) {
        WorkspaceInfo ws = workspaces.findById(id, WorkspaceInfo.class);
        return verifyBeforeReturning(ws, WorkspaceInfo.class);
    }

    public @Override WorkspaceInfo getWorkspaceByName(String name) {
        WorkspaceInfo ws = workspaces.findFirstByName(name, WorkspaceInfo.class);
        return verifyBeforeReturning(ws, WorkspaceInfo.class);
    }

    //
    // Styles
    //
    public @Override StyleInfo add(StyleInfo style) {
        return add(style, StyleInfo.class, styles);
    }

    public @Override void remove(StyleInfo style) {
        styles.remove(unwrap(style));
    }

    /** Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo, Patch)} */
    public @Override void save(StyleInfo style) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    public @Override StyleInfo detach(StyleInfo style) {
        return style;
    }

    public @Override StyleInfo getStyle(String id) {
        StyleInfo match = styles.findById(id, StyleInfo.class);
        return verifyBeforeReturning(match, StyleInfo.class);
    }

    public @Override StyleInfo getStyleByName(String name) {
        StyleInfo match = styles.findByNameAndWordkspaceNull(name);
        if (match == null) {
            match = styles.findFirstByName(name, StyleInfo.class);
        }
        return verifyBeforeReturning(match, StyleInfo.class);
    }

    public @Override StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        Objects.requireNonNull(workspace, "workspace");
        Objects.requireNonNull(name, "name");

        if (workspace == ANY_WORKSPACE) {
            return getStyleByName(name);
        }
        StyleInfo match;
        if (workspace == NO_WORKSPACE) {
            match = styles.findByNameAndWordkspaceNull(name);
        } else {
            match = styles.findByNameAndWordkspace(name, workspace);
        }
        return verifyBeforeReturning(match, StyleInfo.class);
    }

    public @Override List<StyleInfo> getStyles() {
        return verifyBeforeReturning(toList(styles::findAll), StyleInfo.class);
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
        return verifyBeforeReturning(toList(() -> matches), StyleInfo.class);
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
        if (dao instanceof AbstractCatalogFacade) {
            // do an optimized sync
            AbstractCatalogFacade other = (AbstractCatalogFacade) dao;
            this.workspaces.syncTo(other.workspaces);
            this.namespaces.syncTo(other.namespaces);
            this.stores.syncTo(other.stores);
            this.resources.syncTo(other.resources);
            this.layers.syncTo(other.layers);
            this.layerGroups.syncTo(other.layerGroups);
            this.styles.syncTo(other.styles);
            this.maps.syncTo(other.maps);
            other.setCatalog(catalog);
        } else {
            // do a manual import
            sync(workspaces::findAll, dao::add);
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
        try (Stream<T> matches = iterable(of, filter, (SortBy[]) null)) {
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
     * @see org.geoserver.catalog.CatalogFacade#canSort(java.lang.Class, java.lang.String)
     */
    public @Override boolean canSort(
            final Class<? extends CatalogInfo> type, final String propertyName) {
        final String[] path = propertyName.split("\\.");
        Class<?> clazz = type;
        for (int i = 0; i < path.length; i++) {
            String property = path[i];
            Method getter;
            try {
                getter = OwsUtils.getter(clazz, property, null);
            } catch (RuntimeException e) {
                return false;
            }
            clazz = getter.getReturnType();
            if (i == path.length - 1) {
                boolean primitive = clazz.isPrimitive();
                boolean comparable = Comparable.class.isAssignableFrom(clazz);
                boolean canSort = primitive || comparable;
                return canSort;
            }
        }
        throw new IllegalStateException("empty property name");
    }

    public @Override <T extends CatalogInfo> CloseableIterator<T> list(
            final Class<T> of,
            final Filter filter,
            @Nullable Integer offset,
            @Nullable Integer count,
            @Nullable SortBy... sortOrder) {

        if (sortOrder != null) {
            for (SortBy so : sortOrder) {
                if (sortOrder != null && !canSort(of, so.getPropertyName().getPropertyName())) {
                    throw new IllegalArgumentException(
                            "Can't sort objects of type "
                                    + of.getName()
                                    + " by "
                                    + so.getPropertyName());
                }
            }
        }

        Stream<T> stream = iterable(of, filter, sortOrder);

        if (offset != null && offset.intValue() > 0) {
            stream = stream.skip(offset.longValue());
        }

        if (count != null && count.intValue() >= 0) {
            stream = stream.limit(count.longValue());
        }

        stream = verifyBeforeReturning(stream, of);
        final Closeable closeable = stream::close;
        return new CloseableIteratorAdapter<T>(stream.iterator(), closeable);
    }

    @SuppressWarnings("unchecked")
    protected <T extends CatalogInfo> Stream<T> iterable(
            final Class<T> of, final Filter filter, final SortBy[] sortByList) {
        Stream<T> all;

        if (NamespaceInfo.class.isAssignableFrom(of)) {
            all = namespaces.findAll(filter).map(of::cast);
        } else if (WorkspaceInfo.class.isAssignableFrom(of)) {
            all = workspaces.findAll(filter).map(of::cast);
        } else if (StoreInfo.class.isAssignableFrom(of)) {
            all = stores.findAll(filter, (Class<StoreInfo>) of).map(of::cast);
        } else if (ResourceInfo.class.isAssignableFrom(of)) {
            all = resources.findAll(filter, (Class<ResourceInfo>) of).map(of::cast);
        } else if (LayerInfo.class.isAssignableFrom(of)) {
            all = layers.findAll(filter).map(of::cast);
        } else if (LayerGroupInfo.class.isAssignableFrom(of)) {
            all = layerGroups.findAll(filter).map(of::cast);
        } else if (PublishedInfo.class.isAssignableFrom(of)) {
            Stream<T> ls = layers.findAll(filter).map(of::cast);
            Stream<T> lgs = layerGroups.findAll(filter).map(of::cast);
            all = Stream.concat(ls, lgs);
        } else if (StyleInfo.class.isAssignableFrom(of)) {
            all = styles.findAll(filter).map(of::cast);
        } else if (MapInfo.class.isAssignableFrom(of)) {
            all = maps.findAll(filter).map(of::cast);
        } else {
            throw new IllegalArgumentException("Unknown type: " + of);
        }

        if (null != sortByList) {
            for (int i = sortByList.length - 1; i >= 0; i--) {
                SortBy sortBy = sortByList[i];
                Ordering<Object> ordering = Ordering.from(comparator(sortBy));
                if (SortOrder.DESCENDING.equals(sortBy.getSortOrder())) {
                    ordering = ordering.reverse();
                }
                all = all.sorted(ordering);
            }
        }

        return all;
    }

    private Comparator<Object> comparator(final SortBy sortOrder) {
        return new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                Object v1 = OwsUtils.get(o1, sortOrder.getPropertyName().getPropertyName());
                Object v2 = OwsUtils.get(o2, sortOrder.getPropertyName().getPropertyName());
                if (v1 == null) {
                    if (v2 == null) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else if (v2 == null) {
                    return 1;
                }
                @SuppressWarnings({"rawtypes", "unchecked"})
                Comparable<Object> c1 = (Comparable) v1;
                @SuppressWarnings({"rawtypes", "unchecked"})
                Comparable<Object> c2 = (Comparable) v2;
                return c1.compareTo(c2);
            }
        };
    }

    //
    // Utilities
    //
    public static <T> T unwrap(T obj) {
        return ModificationProxy.unwrap(obj);
    }

    protected <T extends CatalogInfo> T verifyBeforeReturning(T ci, Class<T> clazz) {
        if (ci == null) return null;
        if (ci instanceof StoreInfoImpl) ((StoreInfoImpl) ci).setCatalog(catalog);
        if (ci instanceof ResourceInfo) ((ResourceInfo) ci).setCatalog(catalog);
        return ModificationProxy.create(ci, clazz);
    }

    protected <T extends CatalogInfo> List<T> verifyBeforeReturning(List<T> list, Class<T> clazz) {
        return ModificationProxy.createList(list, clazz);
    }

    protected <T extends CatalogInfo> Stream<T> verifyBeforeReturning(
            Stream<T> stream, Class<T> clazz) {
        return stream.map(i -> verifyBeforeReturning(i, clazz));
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

    protected LayerInfo resolve(LayerInfo layer) {
        ResourceInfo resource = ResolvingProxy.resolve(getCatalog(), layer.getResource());
        if (resource != null) {
            resource = unwrap(resource);
            layer.setResource(resource);
        }

        StyleInfo style = ResolvingProxy.resolve(getCatalog(), layer.getDefaultStyle());
        if (style != null) {
            style = unwrap(style);
            layer.setDefaultStyle(style);
        }

        LinkedHashSet<StyleInfo> styles = new LinkedHashSet<StyleInfo>();
        for (StyleInfo s : layer.getStyles()) {
            s = ResolvingProxy.resolve(getCatalog(), s);
            s = unwrap(s);
            styles.add(s);
        }
        if (layer instanceof LayerInfoImpl) {
            ((LayerInfoImpl) layer).setStyles(styles);
        } else {
            layer.getStyles().clear();
            layer.getStyles().addAll(styles);
        }
        return layer;
    }

    protected LayerGroupInfo resolve(LayerGroupInfo lg) {
        for (int i = 0; i < lg.getLayers().size(); i++) {
            PublishedInfo l = lg.getLayers().get(i);

            if (l != null) {
                PublishedInfo resolved;
                if (l instanceof LayerGroupInfo) {
                    resolved = unwrap(ResolvingProxy.resolve(getCatalog(), (LayerGroupInfo) l));
                    // special case to handle catalog loading, when nested publishibles might not be
                    // loaded.
                    if (resolved == null) {
                        resolved = l;
                    }
                } else if (l instanceof LayerInfo) {
                    resolved = unwrap(ResolvingProxy.resolve(getCatalog(), (LayerInfo) l));
                    // special case to handle catalog loading, when nested publishibles might not be
                    // loaded.
                    if (resolved == null) {
                        resolved = l;
                    }
                } else {
                    // Special case for null layer (style group)
                    resolved = unwrap(ResolvingProxy.resolve(getCatalog(), l));
                }
                lg.getLayers().set(i, resolved);
            }
        }

        for (int i = 0; i < lg.getStyles().size(); i++) {
            StyleInfo s = lg.getStyles().get(i);
            if (s != null) {
                StyleInfo resolved = unwrap(ResolvingProxy.resolve(getCatalog(), s));
                lg.getStyles().set(i, resolved);
            }
        }
        return lg;
    }

    protected StyleInfo resolve(StyleInfo style) {
        // resolve the workspace
        WorkspaceInfo ws = style.getWorkspace();
        if (ws != null) {
            WorkspaceInfo resolved = ResolvingProxy.resolve(getCatalog(), ws);
            if (resolved != null) {
                resolved = unwrap(resolved);
                style.setWorkspace(resolved);
            } else {
                LOGGER.log(
                        Level.INFO,
                        "Failed to resolve workspace for style \""
                                + style.getName()
                                + "\". This means the workspace has not yet been added to the catalog, keep the proxy around");
            }
        }
        return style;
    }

    protected MapInfo resolve(MapInfo map) {
        return map;
    }

    protected WorkspaceInfo resolve(WorkspaceInfo workspace) {
        return workspace;
    }

    protected NamespaceInfo resolve(NamespaceInfo namespace) {
        return namespace;
    }

    protected StoreInfo resolve(StoreInfo store) {
        // resolve the workspace
        WorkspaceInfo resolved = ResolvingProxy.resolve(getCatalog(), store.getWorkspace());
        if (resolved != null) {
            resolved = unwrap(resolved);
            store.setWorkspace(resolved);
        } else {
            LOGGER.log(
                    Level.INFO,
                    "Failed to resolve workspace for store \""
                            + store.getName()
                            + "\". This means the workspace has not yet been added to the catalog, keep the proxy around");
        }
        return store;
    }

    protected <R extends ResourceInfo> R resolve(R resource) {
        // resolve the store
        StoreInfo store = ResolvingProxy.resolve(getCatalog(), resource.getStore());
        if (store != null) {
            store = unwrap(store);
            resource.setStore(store);
        }

        // resolve the namespace
        NamespaceInfo namespace = ResolvingProxy.resolve(getCatalog(), resource.getNamespace());
        if (namespace != null) {
            namespace = unwrap(namespace);
            resource.setNamespace(namespace);
        }
        return resource;
    }

    protected void setId(CatalogInfo o) {
        if (OwsUtils.get(o, "id") == null) {
            String uid = new UID().toString();
            OwsUtils.set(o, "id", o.getClass().getSimpleName() + "-" + uid);
        }
    }

    public <I extends CatalogInfo> I update(I info, Patch patch) {
        checkNotAProxy(info);
        CatalogInfoRepository<I> repo = repo(info.getClass());
        return repo.update(info, patch);
    }

    @SuppressWarnings("unchecked")
    protected <I extends CatalogInfo, R extends CatalogInfoRepository<I>> R repo(
            Class<? extends CatalogInfo> type) {
        ClassMappings cm = ClassMappings.fromImpl(type);
        if (cm == null) cm = ClassMappings.fromInterface(type);
        return (R) repos.get(cm).get();
    }

    private static void checkNotAProxy(CatalogInfo value) {
        if (Proxy.isProxyClass(value.getClass())) {
            throw new IllegalArgumentException(
                    "Proxy values shall not be passed to CatalogInfoLookup");
        }
    }
}
