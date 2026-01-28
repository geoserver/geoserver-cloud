/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.forwarding;

import jakarta.annotation.Nullable;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogCapabilities;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;

/**
 * A decorator for {@link CatalogFacade} that forwards all method calls to an underlying facade instance.
 *
 * <p>This class facilitates the creation of decorators by delegating all operations to a subject
 * {@link CatalogFacade}, allowing subclasses to override specific methods to customize behavior (e.g.,
 * adding logging, caching, or validation). It serves as a foundation for extending facade functionality
 * without altering the core implementation.
 *
 * <p>Subclasses should override one or more methods to modify the behavior of the backing facade as needed.
 *
 * <p>Example usage:
 * <pre>
 * CatalogFacade baseFacade = ...;
 * ForwardingCatalogFacade decorator = new ForwardingCatalogFacade(baseFacade) {
 *     &#64;Override
 *     public StoreInfo add(StoreInfo store) {
 *         // Custom logic before forwarding
 *         return super.add(store);
 *     }
 * };
 * </pre>
 *
 * @since 1.0
 * @see CatalogFacade
 */
public class ForwardingCatalogFacade implements CatalogFacade {

    // wrapped catalog facade
    protected final CatalogFacade facade;

    /**
     * Constructs a forwarding facade wrapping the provided subject.
     *
     * @param facade The underlying {@link CatalogFacade} to forward calls to; may be null (behavior depends on subclass).
     */
    public ForwardingCatalogFacade(CatalogFacade facade) {
        this.facade = facade;
    }

    /**
     * Returns this decorator's subject facade.
     *
     * <p>Provides access to the underlying {@link CatalogFacade} instance being decorated.
     *
     * @return The subject {@link CatalogFacade}; may be null if not set.
     */
    public CatalogFacade getSubject() {
        // if you're wondering, I refuse to derive from org.geotools.util.decorate.AbstractDecorator
        // and by extension from java.sql.Wrapper
        return facade;
    }

    /** {@inheritDoc} */
    @Override
    public Catalog getCatalog() {
        return facade.getCatalog();
    }

    /** {@inheritDoc} */
    @Override
    public void setCatalog(Catalog catalog) {
        facade.setCatalog(catalog);
    }

    /** {@inheritDoc} */
    @Override
    public StoreInfo add(StoreInfo store) {
        return facade.add(store);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(StoreInfo store) {
        facade.remove(store);
    }

    /** {@inheritDoc} */
    @Override
    public void save(StoreInfo store) {
        facade.save(store);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> T detach(T store) {
        return facade.detach(store);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return facade.getStore(id, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace, String name, Class<T> clazz) {
        return facade.getStoreByName(workspace, name, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(WorkspaceInfo workspace, Class<T> clazz) {
        return facade.getStoresByWorkspace(workspace, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return facade.getStores(clazz);
    }

    /** {@inheritDoc} */
    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return facade.getDefaultDataStore(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        facade.setDefaultDataStore(workspace, store);
    }

    /** {@inheritDoc} */
    @Override
    public ResourceInfo add(ResourceInfo resource) {
        return facade.add(resource);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(ResourceInfo resource) {
        facade.remove(resource);
    }

    /** {@inheritDoc} */
    @Override
    public void save(ResourceInfo resource) {
        facade.save(resource);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> T detach(T resource) {
        return facade.detach(resource);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return facade.getResource(id, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> T getResourceByName(NamespaceInfo namespace, String name, Class<T> clazz) {
        return facade.getResourceByName(namespace, name, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return facade.getResources(clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(NamespaceInfo namespace, Class<T> clazz) {
        return facade.getResourcesByNamespace(namespace, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> T getResourceByStore(StoreInfo store, String name, Class<T> clazz) {
        return facade.getResourceByStore(store, name, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return facade.getResourcesByStore(store, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public LayerInfo add(LayerInfo layer) {
        return facade.add(layer);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(LayerInfo layer) {
        facade.remove(layer);
    }

    /** {@inheritDoc} */
    @Override
    public void save(LayerInfo layer) {
        facade.save(layer);
    }

    /** {@inheritDoc} */
    @Override
    public LayerInfo detach(LayerInfo layer) {
        return facade.detach(layer);
    }

    /** {@inheritDoc} */
    @Override
    public LayerInfo getLayer(String id) {
        return facade.getLayer(id);
    }

    /** {@inheritDoc} */
    @Override
    public LayerInfo getLayerByName(String name) {
        return facade.getLayerByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return facade.getLayers(resource);
    }

    /** {@inheritDoc} */
    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {
        return facade.getLayers(style);
    }

    /** {@inheritDoc} */
    @Override
    public List<LayerInfo> getLayers() {
        return facade.getLayers();
    }

    /** {@inheritDoc} */
    @Override
    public MapInfo add(MapInfo map) {
        return facade.add(map);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(MapInfo map) {
        facade.remove(map);
    }

    /** {@inheritDoc} */
    @Override
    public void save(MapInfo map) {
        facade.save(map);
    }

    /** {@inheritDoc} */
    @Override
    public MapInfo detach(MapInfo map) {
        return facade.detach(map);
    }

    /** {@inheritDoc} */
    @Override
    public MapInfo getMap(String id) {
        return facade.getMap(id);
    }

    /** {@inheritDoc} */
    @Override
    public MapInfo getMapByName(String name) {
        return facade.getMapByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public List<MapInfo> getMaps() {
        return facade.getMaps();
    }

    /** {@inheritDoc} */
    @Override
    public LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return facade.add(layerGroup);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(LayerGroupInfo layerGroup) {
        facade.remove(layerGroup);
    }

    /** {@inheritDoc} */
    @Override
    public void save(LayerGroupInfo layerGroup) {
        facade.save(layerGroup);
    }

    /** {@inheritDoc} */
    @Override
    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return facade.detach(layerGroup);
    }

    /** {@inheritDoc} */
    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return facade.getLayerGroup(id);
    }

    /** {@inheritDoc} */
    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return facade.getLayerGroupByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return facade.getLayerGroupByName(workspace, name);
    }

    /** {@inheritDoc} */
    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return facade.getLayerGroups();
    }

    /** {@inheritDoc} */
    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return facade.getLayerGroupsByWorkspace(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public NamespaceInfo add(NamespaceInfo namespace) {
        return facade.add(namespace);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(NamespaceInfo namespace) {
        facade.remove(namespace);
    }

    /** {@inheritDoc} */
    @Override
    public void save(NamespaceInfo namespace) {
        facade.save(namespace);
    }

    /** {@inheritDoc} */
    @Override
    public NamespaceInfo detach(NamespaceInfo namespace) {
        return facade.detach(namespace);
    }

    /** {@inheritDoc} */
    @Override
    public NamespaceInfo getDefaultNamespace() {
        return facade.getDefaultNamespace();
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        facade.setDefaultNamespace(defaultNamespace);
    }

    /** {@inheritDoc} */
    @Override
    public NamespaceInfo getNamespace(String id) {
        return facade.getNamespace(id);
    }

    /** {@inheritDoc} */
    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return facade.getNamespaceByPrefix(prefix);
    }

    /** {@inheritDoc} */
    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        return facade.getNamespaceByURI(uri);
    }

    /** {@inheritDoc} */
    @Override
    public List<NamespaceInfo> getNamespacesByURI(String uri) {
        return facade.getNamespacesByURI(uri);
    }

    /** {@inheritDoc} */
    @Override
    public List<NamespaceInfo> getNamespaces() {
        return facade.getNamespaces();
    }

    /** {@inheritDoc} */
    @Override
    public WorkspaceInfo add(WorkspaceInfo workspace) {
        return facade.add(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(WorkspaceInfo workspace) {
        facade.remove(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public void save(WorkspaceInfo workspace) {
        facade.save(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public WorkspaceInfo detach(WorkspaceInfo workspace) {
        return facade.detach(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return facade.getDefaultWorkspace();
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        facade.setDefaultWorkspace(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return facade.getWorkspace(id);
    }

    /** {@inheritDoc} */
    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        return facade.getWorkspaceByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public List<WorkspaceInfo> getWorkspaces() {
        return facade.getWorkspaces();
    }

    /** {@inheritDoc} */
    @Override
    public StyleInfo add(StyleInfo style) {
        return facade.add(style);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(StyleInfo style) {
        facade.remove(style);
    }

    /** {@inheritDoc} */
    @Override
    public void save(StyleInfo style) {
        facade.save(style);
    }

    /** {@inheritDoc} */
    @Override
    public StyleInfo detach(StyleInfo style) {
        return facade.detach(style);
    }

    /** {@inheritDoc} */
    @Override
    public StyleInfo getStyle(String id) {
        return facade.getStyle(id);
    }

    /** {@inheritDoc} */
    @Override
    public StyleInfo getStyleByName(String name) {
        return facade.getStyleByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return facade.getStyleByName(workspace, name);
    }

    /** {@inheritDoc} */
    @Override
    public List<StyleInfo> getStyles() {
        return facade.getStyles();
    }

    /** {@inheritDoc} */
    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return facade.getStylesByWorkspace(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        facade.dispose();
    }

    /** {@inheritDoc} */
    @Override
    public void resolve() {
        facade.resolve();
    }

    /** {@inheritDoc} */
    @Override
    public void syncTo(CatalogFacade other) {
        facade.syncTo(other);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        return facade.count(of, filter);
    }

    /** {@inheritDoc} */
    @Override
    public boolean canSort(Class<? extends CatalogInfo> type, String propertyName) {
        return facade.canSort(type, propertyName);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of,
            Filter filter,
            @Nullable Integer offset,
            @Nullable Integer count,
            @Nullable SortBy... sortOrder) {
        return facade.list(of, filter, offset, count, sortOrder);
    }

    /** {@inheritDoc} */
    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        return facade.getCatalogCapabilities();
    }
}
