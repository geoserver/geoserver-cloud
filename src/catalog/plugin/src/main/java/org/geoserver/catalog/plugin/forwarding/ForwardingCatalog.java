/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;

/**
 * A decorator for {@link Catalog} that forwards all method calls to an underlying catalog instance.
 *
 * <p>This class aids in implementing decorators by providing a base that delegates all operations to a
 * subject {@link Catalog}, allowing subclasses to override specific methods to modify behavior as needed.
 * It’s useful for adding functionality (e.g., logging, caching) without altering the core catalog
 * implementation. The decorator is {@link Serializable} if the subject catalog is serializable.
 *
 * <p>Subclasses should override one or more methods to modify the behavior of the backing catalog as needed.
 *
 * <p>Example usage:
 * <pre>
 * Catalog baseCatalog = ...;
 * ForwardingCatalog decorator = new ForwardingCatalog(baseCatalog) {
 *     &#64;Override
 *     public void add(StoreInfo store) {
 *         // Add custom logic before forwarding
 *         super.add(store);
 *     }
 * };
 * </pre>
 *
 * @since 1.0
 * @see Catalog
 */
@SuppressWarnings("serial")
public class ForwardingCatalog implements Catalog {

    protected final Catalog catalog;

    /**
     * Constructs a forwarding catalog wrapping the provided subject.
     *
     * @param catalog The underlying {@link Catalog} to forward calls to; must not be null.
     * @throws NullPointerException if {@code catalog} is null.
     */
    public ForwardingCatalog(Catalog catalog) {
        Objects.requireNonNull(catalog, "Subject catalog must not be null");
        this.catalog = catalog;
    }

    /**
     * Returns this decorator's subject catalog.
     *
     * <p>Provides access to the underlying {@link Catalog} instance being decorated.
     *
     * @return The subject {@link Catalog}; never null.
     */
    public Catalog getSubject() {
        // if you're wondering, I refuse to derive from org.geotools.util.decorate.AbstractDecorator
        // and by extension from java.sql.Wrapper
        return catalog;
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return catalog.getId();
    }

    /** {@inheritDoc} */
    @Override
    public void accept(CatalogVisitor visitor) {
        catalog.accept(visitor);
    }

    /** {@inheritDoc} */
    @Override
    public Date getDateModified() {
        return catalog.getDateModified();
    }

    /** {@inheritDoc} */
    @Override
    public Date getDateCreated() {
        return catalog.getDateCreated();
    }

    /** {@inheritDoc} */
    @Override
    public void setDateCreated(Date dateCreated) {
        catalog.setDateCreated(dateCreated);
    }

    /** {@inheritDoc} */
    @Override
    public void setDateModified(Date dateModified) {
        catalog.setDateModified(dateModified);
    }

    /** {@inheritDoc} */
    @Override
    public CatalogFacade getFacade() {
        return catalog.getFacade();
    }

    /** {@inheritDoc} */
    @Override
    public CatalogFactory getFactory() {
        return catalog.getFactory();
    }

    /** {@inheritDoc} */
    @Override
    public void add(StoreInfo store) {
        catalog.add(store);
    }

    /** {@inheritDoc} */
    @Override
    public ValidationResult validate(StoreInfo store, boolean isNew) {
        return catalog.validate(store, isNew);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(StoreInfo store) {
        catalog.remove(store);
    }

    /** {@inheritDoc} */
    @Override
    public void save(StoreInfo store) {
        catalog.save(store);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return catalog.getStore(id, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> T getStoreByName(String name, Class<T> clazz) {
        return catalog.getStoreByName(name, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> T getStoreByName(String workspaceName, String name, Class<T> clazz) {
        return catalog.getStoreByName(workspaceName, name, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace, String name, Class<T> clazz) {
        return catalog.getStoreByName(workspace, name, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return catalog.getStores(clazz);
    }

    /** {@inheritDoc} */
    @Override
    public WMSStoreInfo getWMSStore(String id) {
        return catalog.getWMSStore(id);
    }

    /** {@inheritDoc} */
    @Override
    public WMSStoreInfo getWMSStoreByName(String name) {
        return catalog.getWMSStoreByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public WMTSStoreInfo getWMTSStore(String id) {
        return catalog.getWMTSStore(id);
    }

    /** {@inheritDoc} */
    @Override
    public WMTSStoreInfo getWMTSStoreByName(String name) {
        return catalog.getWMTSStoreByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(WorkspaceInfo workspace, Class<T> clazz) {
        return catalog.getStoresByWorkspace(workspace, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(String workspaceName, Class<T> clazz) {
        return catalog.getStoresByWorkspace(workspaceName, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public DataStoreInfo getDataStore(String id) {
        return catalog.getDataStore(id);
    }

    /** {@inheritDoc} */
    @Override
    public DataStoreInfo getDataStoreByName(String name) {
        return catalog.getDataStoreByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public DataStoreInfo getDataStoreByName(String workspaceName, String name) {
        return catalog.getDataStoreByName(workspaceName, name);
    }

    /** {@inheritDoc} */
    @Override
    public DataStoreInfo getDataStoreByName(WorkspaceInfo workspace, String name) {
        return catalog.getDataStoreByName(workspace, name);
    }

    /** {@inheritDoc} */
    @Override
    public List<DataStoreInfo> getDataStoresByWorkspace(String workspaceName) {
        return catalog.getDataStoresByWorkspace(workspaceName);
    }

    /** {@inheritDoc} */
    @Override
    public List<DataStoreInfo> getDataStoresByWorkspace(WorkspaceInfo workspace) {
        return catalog.getDataStoresByWorkspace(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public List<DataStoreInfo> getDataStores() {
        return catalog.getDataStores();
    }

    /** {@inheritDoc} */
    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return catalog.getDefaultDataStore(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo defaultStore) {
        catalog.setDefaultDataStore(workspace, defaultStore);
    }

    /** {@inheritDoc} */
    @Override
    public CoverageStoreInfo getCoverageStore(String id) {
        return catalog.getCoverageStore(id);
    }

    /** {@inheritDoc} */
    @Override
    public CoverageStoreInfo getCoverageStoreByName(String name) {
        return catalog.getCoverageStoreByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public CoverageStoreInfo getCoverageStoreByName(String workspaceName, String name) {
        return catalog.getCoverageStoreByName(workspaceName, name);
    }

    /** {@inheritDoc} */
    @Override
    public CoverageStoreInfo getCoverageStoreByName(WorkspaceInfo workspace, String name) {
        return catalog.getCoverageStoreByName(workspace, name);
    }

    /** {@inheritDoc} */
    @Override
    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(String workspaceName) {
        return catalog.getCoverageStoresByWorkspace(workspaceName);
    }

    /** {@inheritDoc} */
    @Override
    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(WorkspaceInfo workspace) {
        return catalog.getCoverageStoresByWorkspace(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public List<CoverageStoreInfo> getCoverageStores() {
        return catalog.getCoverageStores();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return catalog.getResource(id, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> T getResourceByName(String ns, String name, Class<T> clazz) {
        return catalog.getResourceByName(ns, name, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> T getResourceByName(NamespaceInfo ns, String name, Class<T> clazz) {
        return catalog.getResourceByName(ns, name, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> T getResourceByName(Name name, Class<T> clazz) {
        return catalog.getResourceByName(name, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> T getResourceByName(String name, Class<T> clazz) {
        return catalog.getResourceByName(name, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public void add(ResourceInfo resource) {
        catalog.add(resource);
    }

    /** {@inheritDoc} */
    @Override
    public ValidationResult validate(ResourceInfo resource, boolean isNew) {
        return catalog.validate(resource, isNew);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(ResourceInfo resource) {
        catalog.remove(resource);
    }

    /** {@inheritDoc} */
    @Override
    public void save(ResourceInfo resource) {
        catalog.save(resource);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> T detach(T resource) {
        return catalog.detach(resource);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> T detach(T store) {
        return catalog.detach(store);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return catalog.getResources(clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(NamespaceInfo namespace, Class<T> clazz) {
        return catalog.getResourcesByNamespace(namespace, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(String namespace, Class<T> clazz) {
        return catalog.getResourcesByNamespace(namespace, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> T getResourceByStore(StoreInfo store, String name, Class<T> clazz) {
        return catalog.getResourceByStore(store, name, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return catalog.getResourcesByStore(store, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public FeatureTypeInfo getFeatureType(String id) {
        return catalog.getFeatureType(id);
    }

    /** {@inheritDoc} */
    @Override
    public FeatureTypeInfo getFeatureTypeByName(String ns, String name) {
        return catalog.getFeatureTypeByName(ns, name);
    }

    /** {@inheritDoc} */
    @Override
    public FeatureTypeInfo getFeatureTypeByName(NamespaceInfo ns, String name) {
        return catalog.getFeatureTypeByName(ns, name);
    }

    /** {@inheritDoc} */
    @Override
    public FeatureTypeInfo getFeatureTypeByName(Name name) {
        return catalog.getFeatureTypeByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public FeatureTypeInfo getFeatureTypeByName(String name) {
        return catalog.getFeatureTypeByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public List<FeatureTypeInfo> getFeatureTypes() {
        return catalog.getFeatureTypes();
    }

    /** {@inheritDoc} */
    @Override
    public List<FeatureTypeInfo> getFeatureTypesByNamespace(NamespaceInfo namespace) {
        return catalog.getFeatureTypesByNamespace(namespace);
    }

    /** {@inheritDoc} */
    @Override
    public FeatureTypeInfo getFeatureTypeByDataStore(DataStoreInfo dataStore, String name) {
        return catalog.getFeatureTypeByDataStore(dataStore, name);
    }

    /** {@inheritDoc} */
    @Override
    public List<FeatureTypeInfo> getFeatureTypesByDataStore(DataStoreInfo store) {
        return catalog.getFeatureTypesByDataStore(store);
    }

    /** {@inheritDoc} */
    @Override
    public CoverageInfo getCoverage(String id) {
        return catalog.getCoverage(id);
    }

    /** {@inheritDoc} */
    @Override
    public CoverageInfo getCoverageByName(String ns, String name) {
        return catalog.getCoverageByName(ns, name);
    }

    /** {@inheritDoc} */
    @Override
    public CoverageInfo getCoverageByName(NamespaceInfo ns, String name) {
        return catalog.getCoverageByName(ns, name);
    }

    /** {@inheritDoc} */
    @Override
    public CoverageInfo getCoverageByName(Name name) {
        return catalog.getCoverageByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public CoverageInfo getCoverageByName(String name) {
        return catalog.getCoverageByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public List<CoverageInfo> getCoverages() {
        return catalog.getCoverages();
    }

    /** {@inheritDoc} */
    @Override
    public List<CoverageInfo> getCoveragesByNamespace(NamespaceInfo namespace) {
        return catalog.getCoveragesByNamespace(namespace);
    }

    /** {@inheritDoc} */
    @Override
    public CoverageInfo getCoverageByCoverageStore(CoverageStoreInfo coverageStore, String name) {
        return catalog.getCoverageByCoverageStore(coverageStore, name);
    }

    /** {@inheritDoc} */
    @Override
    public List<CoverageInfo> getCoveragesByCoverageStore(CoverageStoreInfo store) {
        return catalog.getCoveragesByCoverageStore(store);
    }

    /** {@inheritDoc} */
    @Override
    public void add(LayerInfo layer) {
        catalog.add(layer);
    }

    /** {@inheritDoc} */
    @Override
    public ValidationResult validate(LayerInfo layer, boolean isNew) {
        return catalog.validate(layer, isNew);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(LayerInfo layer) {
        catalog.remove(layer);
    }

    /** {@inheritDoc} */
    @Override
    public void save(LayerInfo layer) {
        catalog.save(layer);
    }

    /** {@inheritDoc} */
    @Override
    public LayerInfo detach(LayerInfo layer) {
        return catalog.detach(layer);
    }

    /** {@inheritDoc} */
    @Override
    public List<CoverageInfo> getCoveragesByStore(CoverageStoreInfo store) {
        return catalog.getCoveragesByStore(store);
    }

    /** {@inheritDoc} */
    @Override
    public LayerInfo getLayer(String id) {
        return catalog.getLayer(id);
    }

    /** {@inheritDoc} */
    @Override
    public LayerInfo getLayerByName(String name) {
        return catalog.getLayerByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public LayerInfo getLayerByName(Name name) {
        return catalog.getLayerByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public List<LayerInfo> getLayers() {
        return catalog.getLayers();
    }

    /** {@inheritDoc} */
    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return catalog.getLayers(resource);
    }

    /** {@inheritDoc} */
    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {
        return catalog.getLayers(style);
    }

    /** {@inheritDoc} */
    @Override
    public void add(MapInfo map) {
        catalog.add(map);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(MapInfo map) {
        catalog.remove(map);
    }

    /** {@inheritDoc} */
    @Override
    public void save(MapInfo map) {
        catalog.save(map);
    }

    /** {@inheritDoc} */
    @Override
    public MapInfo detach(MapInfo map) {
        return catalog.detach(map);
    }

    /** {@inheritDoc} */
    @Override
    public List<MapInfo> getMaps() {
        return catalog.getMaps();
    }

    /** {@inheritDoc} */
    @Override
    public MapInfo getMap(String id) {
        return catalog.getMap(id);
    }

    /** {@inheritDoc} */
    @Override
    public MapInfo getMapByName(String name) {
        return catalog.getMapByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public void add(LayerGroupInfo layerGroup) {
        catalog.add(layerGroup);
    }

    /** {@inheritDoc} */
    @Override
    public ValidationResult validate(LayerGroupInfo layerGroup, boolean isNew) {
        return catalog.validate(layerGroup, isNew);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(LayerGroupInfo layerGroup) {
        catalog.remove(layerGroup);
    }

    /** {@inheritDoc} */
    @Override
    public void save(LayerGroupInfo layerGroup) {
        catalog.save(layerGroup);
    }

    /** {@inheritDoc} */
    @Override
    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return catalog.detach(layerGroup);
    }

    /** {@inheritDoc} */
    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return catalog.getLayerGroups();
    }

    /** {@inheritDoc} */
    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName) {
        return catalog.getLayerGroupsByWorkspace(workspaceName);
    }

    /** {@inheritDoc} */
    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return catalog.getLayerGroupsByWorkspace(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return catalog.getLayerGroup(id);
    }

    /** {@inheritDoc} */
    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return catalog.getLayerGroupByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public LayerGroupInfo getLayerGroupByName(String workspaceName, String name) {
        return catalog.getLayerGroupByName(workspaceName, name);
    }

    /** {@inheritDoc} */
    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return catalog.getLayerGroupByName(workspace, name);
    }

    /** {@inheritDoc} */
    @Override
    public void add(StyleInfo style) {
        catalog.add(style);
    }

    /** {@inheritDoc} */
    @Override
    public ValidationResult validate(StyleInfo style, boolean isNew) {
        return catalog.validate(style, isNew);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(StyleInfo style) {
        catalog.remove(style);
    }

    /** {@inheritDoc} */
    @Override
    public void save(StyleInfo style) {
        catalog.save(style);
    }

    /** {@inheritDoc} */
    @Override
    public StyleInfo detach(StyleInfo style) {
        return catalog.detach(style);
    }

    /** {@inheritDoc} */
    @Override
    public StyleInfo getStyle(String id) {
        return catalog.getStyle(id);
    }

    /** {@inheritDoc} */
    @Override
    public StyleInfo getStyleByName(String workspaceName, String name) {
        return catalog.getStyleByName(workspaceName, name);
    }

    /** {@inheritDoc} */
    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return catalog.getStyleByName(workspace, name);
    }

    /** {@inheritDoc} */
    @Override
    public StyleInfo getStyleByName(String name) {
        return catalog.getStyleByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public List<StyleInfo> getStyles() {
        return catalog.getStyles();
    }

    /** {@inheritDoc} */
    @Override
    public List<StyleInfo> getStylesByWorkspace(String workspaceName) {
        return catalog.getStylesByWorkspace(workspaceName);
    }

    /** {@inheritDoc} */
    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return catalog.getStylesByWorkspace(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public void add(NamespaceInfo namespace) {
        catalog.add(namespace);
    }

    /** {@inheritDoc} */
    @Override
    public ValidationResult validate(NamespaceInfo namespace, boolean isNew) {
        return catalog.validate(namespace, isNew);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(NamespaceInfo namespace) {
        catalog.remove(namespace);
    }

    /** {@inheritDoc} */
    @Override
    public void save(NamespaceInfo namespace) {
        catalog.save(namespace);
    }

    /** {@inheritDoc} */
    @Override
    public NamespaceInfo detach(NamespaceInfo namespace) {
        return catalog.detach(namespace);
    }

    /** {@inheritDoc} */
    @Override
    public NamespaceInfo getNamespace(String id) {
        return catalog.getNamespace(id);
    }

    /** {@inheritDoc} */
    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return catalog.getNamespaceByPrefix(prefix);
    }

    /** {@inheritDoc} */
    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        return catalog.getNamespaceByURI(uri);
    }

    /** {@inheritDoc} */
    @Override
    public NamespaceInfo getDefaultNamespace() {
        return catalog.getDefaultNamespace();
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        catalog.setDefaultNamespace(defaultNamespace);
    }

    /** {@inheritDoc} */
    @Override
    public List<NamespaceInfo> getNamespaces() {
        return catalog.getNamespaces();
    }

    /** {@inheritDoc} */
    @Override
    public void add(WorkspaceInfo workspace) {
        catalog.add(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public ValidationResult validate(WorkspaceInfo workspace, boolean isNew) {
        return catalog.validate(workspace, isNew);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(WorkspaceInfo workspace) {
        catalog.remove(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public void save(WorkspaceInfo workspace) {
        catalog.save(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public WorkspaceInfo detach(WorkspaceInfo workspace) {
        return catalog.detach(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return catalog.getDefaultWorkspace();
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        catalog.setDefaultWorkspace(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public List<WorkspaceInfo> getWorkspaces() {
        return catalog.getWorkspaces();
    }

    /** {@inheritDoc} */
    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return catalog.getWorkspace(id);
    }

    /** {@inheritDoc} */
    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        return catalog.getWorkspaceByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<CatalogListener> getListeners() {
        return catalog.getListeners();
    }

    /** {@inheritDoc} */
    @Override
    public void addListener(CatalogListener listener) {
        catalog.addListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void removeListener(CatalogListener listener) {
        catalog.removeListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void fireAdded(CatalogInfo object) {
        catalog.fireAdded(object);
    }

    /** {@inheritDoc} */
    @Override
    public void fireModified(
            CatalogInfo object, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {
        catalog.fireModified(object, propertyNames, oldValues, newValues);
    }

    /** {@inheritDoc} */
    @Override
    public void firePostModified(
            CatalogInfo object, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {
        catalog.firePostModified(object, propertyNames, oldValues, newValues);
    }

    /** {@inheritDoc} */
    @Override
    public void fireRemoved(CatalogInfo object) {
        catalog.fireRemoved(object);
    }

    /** {@inheritDoc} */
    @Override
    public ResourcePool getResourcePool() {
        return catalog.getResourcePool();
    }

    /** {@inheritDoc} */
    @Override
    public void setResourcePool(ResourcePool resourcePool) {
        catalog.setResourcePool(resourcePool);
    }

    /** {@inheritDoc} */
    @Override
    public GeoServerResourceLoader getResourceLoader() {
        return catalog.getResourceLoader();
    }

    /** {@inheritDoc} */
    @Override
    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        catalog.setResourceLoader(resourceLoader);
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        catalog.dispose();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        return catalog.count(of, filter);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CatalogInfo> T get(Class<T> type, Filter filter) throws IllegalArgumentException {
        return catalog.get(type, filter);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(Class<T> of, Filter filter) {
        return catalog.list(of, filter);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of, Filter filter, Integer offset, Integer count, SortBy sortBy) {
        return catalog.list(of, filter, offset, count, sortBy);
    }

    /** {@inheritDoc} */
    @Override
    public void removeListeners(Class<? extends CatalogListener> listenerClass) {
        catalog.removeListeners(listenerClass);
    }

    /** {@inheritDoc} */
    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        return catalog.getCatalogCapabilities();
    }
}
