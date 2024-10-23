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
 * {@link Catalog} which forwards all its method calls to another {@code Catalog} aiding in
 * implementing a decorator.
 *
 * <p>Subclasses should override one or more methods to modify the behavior of the backing facade as
 * needed.
 *
 * <p>The catalog is {@link Serializable serializable} if the subject catalog is so.
 */
public class ForwardingCatalog implements Catalog {

    private static final long serialVersionUID = 1L;

    protected final Catalog catalog;

    public ForwardingCatalog(Catalog catalog) {
        Objects.requireNonNull(catalog);
        this.catalog = catalog;
    }

    /**
     * @return this decorator's subject
     */
    public Catalog getSubject() {
        // if you're wondering, I refuse to derive from org.geotools.util.decorate.AbstractDecorator
        // and by extension from java.sql.Wrapper
        return catalog;
    }

    @Override
    public String getId() {
        return catalog.getId();
    }

    @Override
    public void accept(CatalogVisitor visitor) {
        catalog.accept(visitor);
    }

    @Override
    public Date getDateModified() {
        return catalog.getDateModified();
    }

    @Override
    public Date getDateCreated() {
        return catalog.getDateCreated();
    }

    @Override
    public void setDateCreated(Date dateCreated) {
        catalog.setDateCreated(dateCreated);
    }

    @Override
    public void setDateModified(Date dateModified) {
        catalog.setDateModified(dateModified);
    }

    @Override
    public CatalogFacade getFacade() {
        return catalog.getFacade();
    }

    @Override
    public CatalogFactory getFactory() {
        return catalog.getFactory();
    }

    @Override
    public void add(StoreInfo store) {
        catalog.add(store);
    }

    @Override
    public ValidationResult validate(StoreInfo store, boolean isNew) {
        return catalog.validate(store, isNew);
    }

    @Override
    public void remove(StoreInfo store) {
        catalog.remove(store);
    }

    @Override
    public void save(StoreInfo store) {
        catalog.save(store);
    }

    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return catalog.getStore(id, clazz);
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(String name, Class<T> clazz) {
        return catalog.getStoreByName(name, clazz);
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(String workspaceName, String name, Class<T> clazz) {
        return catalog.getStoreByName(workspaceName, name, clazz);
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace, String name, Class<T> clazz) {
        return catalog.getStoreByName(workspace, name, clazz);
    }

    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return catalog.getStores(clazz);
    }

    @Override
    public WMSStoreInfo getWMSStore(String id) {
        return catalog.getWMSStore(id);
    }

    @Override
    public WMSStoreInfo getWMSStoreByName(String name) {
        return catalog.getWMSStoreByName(name);
    }

    @Override
    public WMTSStoreInfo getWMTSStore(String id) {
        return catalog.getWMTSStore(id);
    }

    @Override
    public WMTSStoreInfo getWMTSStoreByName(String name) {
        return catalog.getWMTSStoreByName(name);
    }

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(WorkspaceInfo workspace, Class<T> clazz) {
        return catalog.getStoresByWorkspace(workspace, clazz);
    }

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(String workspaceName, Class<T> clazz) {
        return catalog.getStoresByWorkspace(workspaceName, clazz);
    }

    @Override
    public DataStoreInfo getDataStore(String id) {
        return catalog.getDataStore(id);
    }

    @Override
    public DataStoreInfo getDataStoreByName(String name) {
        return catalog.getDataStoreByName(name);
    }

    @Override
    public DataStoreInfo getDataStoreByName(String workspaceName, String name) {
        return catalog.getDataStoreByName(workspaceName, name);
    }

    @Override
    public DataStoreInfo getDataStoreByName(WorkspaceInfo workspace, String name) {
        return catalog.getDataStoreByName(workspace, name);
    }

    @Override
    public List<DataStoreInfo> getDataStoresByWorkspace(String workspaceName) {
        return catalog.getDataStoresByWorkspace(workspaceName);
    }

    @Override
    public List<DataStoreInfo> getDataStoresByWorkspace(WorkspaceInfo workspace) {
        return catalog.getDataStoresByWorkspace(workspace);
    }

    @Override
    public List<DataStoreInfo> getDataStores() {
        return catalog.getDataStores();
    }

    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return catalog.getDefaultDataStore(workspace);
    }

    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo defaultStore) {
        catalog.setDefaultDataStore(workspace, defaultStore);
    }

    @Override
    public CoverageStoreInfo getCoverageStore(String id) {
        return catalog.getCoverageStore(id);
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(String name) {
        return catalog.getCoverageStoreByName(name);
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(String workspaceName, String name) {
        return catalog.getCoverageStoreByName(workspaceName, name);
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(WorkspaceInfo workspace, String name) {
        return catalog.getCoverageStoreByName(workspace, name);
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(String workspaceName) {
        return catalog.getCoverageStoresByWorkspace(workspaceName);
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(WorkspaceInfo workspace) {
        return catalog.getCoverageStoresByWorkspace(workspace);
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStores() {
        return catalog.getCoverageStores();
    }

    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return catalog.getResource(id, clazz);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(String ns, String name, Class<T> clazz) {
        return catalog.getResourceByName(ns, name, clazz);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(NamespaceInfo ns, String name, Class<T> clazz) {
        return catalog.getResourceByName(ns, name, clazz);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(Name name, Class<T> clazz) {
        return catalog.getResourceByName(name, clazz);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(String name, Class<T> clazz) {
        return catalog.getResourceByName(name, clazz);
    }

    @Override
    public void add(ResourceInfo resource) {
        catalog.add(resource);
    }

    @Override
    public ValidationResult validate(ResourceInfo resource, boolean isNew) {
        return catalog.validate(resource, isNew);
    }

    @Override
    public void remove(ResourceInfo resource) {
        catalog.remove(resource);
    }

    @Override
    public void save(ResourceInfo resource) {
        catalog.save(resource);
    }

    @Override
    public <T extends ResourceInfo> T detach(T resource) {
        return catalog.detach(resource);
    }

    @Override
    public <T extends StoreInfo> T detach(T store) {
        return catalog.detach(store);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return catalog.getResources(clazz);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(NamespaceInfo namespace, Class<T> clazz) {
        return catalog.getResourcesByNamespace(namespace, clazz);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(String namespace, Class<T> clazz) {
        return catalog.getResourcesByNamespace(namespace, clazz);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByStore(StoreInfo store, String name, Class<T> clazz) {
        return catalog.getResourceByStore(store, name, clazz);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return catalog.getResourcesByStore(store, clazz);
    }

    @Override
    public FeatureTypeInfo getFeatureType(String id) {
        return catalog.getFeatureType(id);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(String ns, String name) {
        return catalog.getFeatureTypeByName(ns, name);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(NamespaceInfo ns, String name) {
        return catalog.getFeatureTypeByName(ns, name);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(Name name) {
        return catalog.getFeatureTypeByName(name);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(String name) {
        return catalog.getFeatureTypeByName(name);
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypes() {
        return catalog.getFeatureTypes();
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypesByNamespace(NamespaceInfo namespace) {
        return catalog.getFeatureTypesByNamespace(namespace);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByDataStore(DataStoreInfo dataStore, String name) {
        return catalog.getFeatureTypeByDataStore(dataStore, name);
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypesByDataStore(DataStoreInfo store) {
        return catalog.getFeatureTypesByDataStore(store);
    }

    @Override
    public CoverageInfo getCoverage(String id) {
        return catalog.getCoverage(id);
    }

    @Override
    public CoverageInfo getCoverageByName(String ns, String name) {
        return catalog.getCoverageByName(ns, name);
    }

    @Override
    public CoverageInfo getCoverageByName(NamespaceInfo ns, String name) {
        return catalog.getCoverageByName(ns, name);
    }

    @Override
    public CoverageInfo getCoverageByName(Name name) {
        return catalog.getCoverageByName(name);
    }

    @Override
    public CoverageInfo getCoverageByName(String name) {
        return catalog.getCoverageByName(name);
    }

    @Override
    public List<CoverageInfo> getCoverages() {
        return catalog.getCoverages();
    }

    @Override
    public List<CoverageInfo> getCoveragesByNamespace(NamespaceInfo namespace) {
        return catalog.getCoveragesByNamespace(namespace);
    }

    @Override
    public CoverageInfo getCoverageByCoverageStore(CoverageStoreInfo coverageStore, String name) {
        return catalog.getCoverageByCoverageStore(coverageStore, name);
    }

    @Override
    public List<CoverageInfo> getCoveragesByCoverageStore(CoverageStoreInfo store) {
        return catalog.getCoveragesByCoverageStore(store);
    }

    @Override
    public void add(LayerInfo layer) {
        catalog.add(layer);
    }

    @Override
    public ValidationResult validate(LayerInfo layer, boolean isNew) {
        return catalog.validate(layer, isNew);
    }

    @Override
    public void remove(LayerInfo layer) {
        catalog.remove(layer);
    }

    @Override
    public void save(LayerInfo layer) {
        catalog.save(layer);
    }

    @Override
    public LayerInfo detach(LayerInfo layer) {
        return catalog.detach(layer);
    }

    @Override
    public List<CoverageInfo> getCoveragesByStore(CoverageStoreInfo store) {
        return catalog.getCoveragesByStore(store);
    }

    @Override
    public LayerInfo getLayer(String id) {
        return catalog.getLayer(id);
    }

    @Override
    public LayerInfo getLayerByName(String name) {
        return catalog.getLayerByName(name);
    }

    @Override
    public LayerInfo getLayerByName(Name name) {
        return catalog.getLayerByName(name);
    }

    @Override
    public List<LayerInfo> getLayers() {
        return catalog.getLayers();
    }

    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return catalog.getLayers(resource);
    }

    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {
        return catalog.getLayers(style);
    }

    @Override
    public void add(MapInfo map) {
        catalog.add(map);
    }

    @Override
    public void remove(MapInfo map) {
        catalog.remove(map);
    }

    @Override
    public void save(MapInfo map) {
        catalog.save(map);
    }

    @Override
    public MapInfo detach(MapInfo map) {
        return catalog.detach(map);
    }

    @Override
    public List<MapInfo> getMaps() {
        return catalog.getMaps();
    }

    @Override
    public MapInfo getMap(String id) {
        return catalog.getMap(id);
    }

    @Override
    public MapInfo getMapByName(String name) {
        return catalog.getMapByName(name);
    }

    @Override
    public void add(LayerGroupInfo layerGroup) {
        catalog.add(layerGroup);
    }

    @Override
    public ValidationResult validate(LayerGroupInfo layerGroup, boolean isNew) {
        return catalog.validate(layerGroup, isNew);
    }

    @Override
    public void remove(LayerGroupInfo layerGroup) {
        catalog.remove(layerGroup);
    }

    @Override
    public void save(LayerGroupInfo layerGroup) {
        catalog.save(layerGroup);
    }

    @Override
    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return catalog.detach(layerGroup);
    }

    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return catalog.getLayerGroups();
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName) {
        return catalog.getLayerGroupsByWorkspace(workspaceName);
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return catalog.getLayerGroupsByWorkspace(workspace);
    }

    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return catalog.getLayerGroup(id);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return catalog.getLayerGroupByName(name);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String workspaceName, String name) {
        return catalog.getLayerGroupByName(workspaceName, name);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return catalog.getLayerGroupByName(workspace, name);
    }

    @Override
    public void add(StyleInfo style) {
        catalog.add(style);
    }

    @Override
    public ValidationResult validate(StyleInfo style, boolean isNew) {
        return catalog.validate(style, isNew);
    }

    @Override
    public void remove(StyleInfo style) {
        catalog.remove(style);
    }

    @Override
    public void save(StyleInfo style) {
        catalog.save(style);
    }

    @Override
    public StyleInfo detach(StyleInfo style) {
        return catalog.detach(style);
    }

    @Override
    public StyleInfo getStyle(String id) {
        return catalog.getStyle(id);
    }

    @Override
    public StyleInfo getStyleByName(String workspaceName, String name) {
        return catalog.getStyleByName(workspaceName, name);
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return catalog.getStyleByName(workspace, name);
    }

    @Override
    public StyleInfo getStyleByName(String name) {
        return catalog.getStyleByName(name);
    }

    @Override
    public List<StyleInfo> getStyles() {
        return catalog.getStyles();
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(String workspaceName) {
        return catalog.getStylesByWorkspace(workspaceName);
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return catalog.getStylesByWorkspace(workspace);
    }

    @Override
    public void add(NamespaceInfo namespace) {
        catalog.add(namespace);
    }

    @Override
    public ValidationResult validate(NamespaceInfo namespace, boolean isNew) {
        return catalog.validate(namespace, isNew);
    }

    @Override
    public void remove(NamespaceInfo namespace) {
        catalog.remove(namespace);
    }

    @Override
    public void save(NamespaceInfo namespace) {
        catalog.save(namespace);
    }

    @Override
    public NamespaceInfo detach(NamespaceInfo namespace) {
        return catalog.detach(namespace);
    }

    @Override
    public NamespaceInfo getNamespace(String id) {
        return catalog.getNamespace(id);
    }

    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return catalog.getNamespaceByPrefix(prefix);
    }

    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        return catalog.getNamespaceByURI(uri);
    }

    @Override
    public NamespaceInfo getDefaultNamespace() {
        return catalog.getDefaultNamespace();
    }

    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        catalog.setDefaultNamespace(defaultNamespace);
    }

    @Override
    public List<NamespaceInfo> getNamespaces() {
        return catalog.getNamespaces();
    }

    @Override
    public void add(WorkspaceInfo workspace) {
        catalog.add(workspace);
    }

    @Override
    public ValidationResult validate(WorkspaceInfo workspace, boolean isNew) {
        return catalog.validate(workspace, isNew);
    }

    @Override
    public void remove(WorkspaceInfo workspace) {
        catalog.remove(workspace);
    }

    @Override
    public void save(WorkspaceInfo workspace) {
        catalog.save(workspace);
    }

    @Override
    public WorkspaceInfo detach(WorkspaceInfo workspace) {
        return catalog.detach(workspace);
    }

    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return catalog.getDefaultWorkspace();
    }

    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        catalog.setDefaultWorkspace(workspace);
    }

    @Override
    public List<WorkspaceInfo> getWorkspaces() {
        return catalog.getWorkspaces();
    }

    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return catalog.getWorkspace(id);
    }

    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        return catalog.getWorkspaceByName(name);
    }

    @Override
    public Collection<CatalogListener> getListeners() {
        return catalog.getListeners();
    }

    @Override
    public void addListener(CatalogListener listener) {
        catalog.addListener(listener);
    }

    @Override
    public void removeListener(CatalogListener listener) {
        catalog.removeListener(listener);
    }

    @Override
    public void fireAdded(CatalogInfo object) {
        catalog.fireAdded(object);
    }

    @Override
    public void fireModified(
            CatalogInfo object, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {
        catalog.fireModified(object, propertyNames, oldValues, newValues);
    }

    @Override
    public void firePostModified(
            CatalogInfo object, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {
        catalog.firePostModified(object, propertyNames, oldValues, newValues);
    }

    @Override
    public void fireRemoved(CatalogInfo object) {
        catalog.fireRemoved(object);
    }

    @Override
    public ResourcePool getResourcePool() {
        return catalog.getResourcePool();
    }

    @Override
    public void setResourcePool(ResourcePool resourcePool) {
        catalog.setResourcePool(resourcePool);
    }

    @Override
    public GeoServerResourceLoader getResourceLoader() {
        return catalog.getResourceLoader();
    }

    @Override
    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        catalog.setResourceLoader(resourceLoader);
    }

    @Override
    public void dispose() {
        catalog.dispose();
    }

    @Override
    public <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        return catalog.count(of, filter);
    }

    @Override
    public <T extends CatalogInfo> T get(Class<T> type, Filter filter) throws IllegalArgumentException {
        return catalog.get(type, filter);
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(Class<T> of, Filter filter) {
        return catalog.list(of, filter);
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of, Filter filter, Integer offset, Integer count, SortBy sortBy) {
        return catalog.list(of, filter, offset, count, sortBy);
    }

    @Override
    public void removeListeners(Class<? extends CatalogListener> listenerClass) {
        catalog.removeListeners(listenerClass);
    }

    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        return catalog.getCatalogCapabilities();
    }
}
