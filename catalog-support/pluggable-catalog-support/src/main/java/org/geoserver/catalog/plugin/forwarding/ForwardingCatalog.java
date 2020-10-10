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
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.platform.GeoServerResourceLoader;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

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

    /** @return this decorator's subject */
    public Catalog getSubject() {
        // if you're wondering, I refuse to derive from org.geotools.util.decorate.AbstractDecorator
        // and by extension from java.sql.Wrapper
        return catalog;
    }

    public @Override String getId() {
        return catalog.getId();
    }

    public @Override void accept(CatalogVisitor visitor) {
        catalog.accept(visitor);
    }

    public @Override Date getDateModified() {
        return catalog.getDateModified();
    }

    public @Override Date getDateCreated() {
        return catalog.getDateCreated();
    }

    public @Override void setDateCreated(Date dateCreated) {
        catalog.setDateCreated(dateCreated);
    }

    public @Override void setDateModified(Date dateModified) {
        catalog.setDateModified(dateModified);
    }

    public @Override CatalogFacade getFacade() {
        return catalog.getFacade();
    }

    public @Override CatalogFactory getFactory() {
        return catalog.getFactory();
    }

    public @Override void add(StoreInfo store) {
        catalog.add(store);
    }

    public @Override ValidationResult validate(StoreInfo store, boolean isNew) {
        return catalog.validate(store, isNew);
    }

    public @Override void remove(StoreInfo store) {
        catalog.remove(store);
    }

    public @Override void save(StoreInfo store) {
        catalog.save(store);
    }

    public @Override <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return catalog.getStore(id, clazz);
    }

    public @Override <T extends StoreInfo> T getStoreByName(String name, Class<T> clazz) {
        return catalog.getStoreByName(name, clazz);
    }

    public @Override <T extends StoreInfo> T getStoreByName(
            String workspaceName, String name, Class<T> clazz) {
        return catalog.getStoreByName(workspaceName, name, clazz);
    }

    public @Override <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {
        return catalog.getStoreByName(workspace, name, clazz);
    }

    public @Override <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return catalog.getStores(clazz);
    }

    public @Override <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        return catalog.getStoresByWorkspace(workspace, clazz);
    }

    public @Override <T extends StoreInfo> List<T> getStoresByWorkspace(
            String workspaceName, Class<T> clazz) {
        return catalog.getStoresByWorkspace(workspaceName, clazz);
    }

    public @Override DataStoreInfo getDataStore(String id) {
        return catalog.getDataStore(id);
    }

    public @Override DataStoreInfo getDataStoreByName(String name) {
        return catalog.getDataStoreByName(name);
    }

    public @Override DataStoreInfo getDataStoreByName(String workspaceName, String name) {
        return catalog.getDataStoreByName(workspaceName, name);
    }

    public @Override DataStoreInfo getDataStoreByName(WorkspaceInfo workspace, String name) {
        return catalog.getDataStoreByName(workspace, name);
    }

    public @Override List<DataStoreInfo> getDataStoresByWorkspace(String workspaceName) {
        return catalog.getDataStoresByWorkspace(workspaceName);
    }

    public @Override List<DataStoreInfo> getDataStoresByWorkspace(WorkspaceInfo workspace) {
        return catalog.getDataStoresByWorkspace(workspace);
    }

    public @Override List<DataStoreInfo> getDataStores() {
        return catalog.getDataStores();
    }

    public @Override DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return catalog.getDefaultDataStore(workspace);
    }

    public @Override void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo defaultStore) {
        catalog.setDefaultDataStore(workspace, defaultStore);
    }

    public @Override CoverageStoreInfo getCoverageStore(String id) {
        return catalog.getCoverageStore(id);
    }

    public @Override CoverageStoreInfo getCoverageStoreByName(String name) {
        return catalog.getCoverageStoreByName(name);
    }

    public @Override CoverageStoreInfo getCoverageStoreByName(String workspaceName, String name) {
        return catalog.getCoverageStoreByName(workspaceName, name);
    }

    public @Override CoverageStoreInfo getCoverageStoreByName(
            WorkspaceInfo workspace, String name) {
        return catalog.getCoverageStoreByName(workspace, name);
    }

    public @Override List<CoverageStoreInfo> getCoverageStoresByWorkspace(String workspaceName) {
        return catalog.getCoverageStoresByWorkspace(workspaceName);
    }

    public @Override List<CoverageStoreInfo> getCoverageStoresByWorkspace(WorkspaceInfo workspace) {
        return catalog.getCoverageStoresByWorkspace(workspace);
    }

    public @Override List<CoverageStoreInfo> getCoverageStores() {
        return catalog.getCoverageStores();
    }

    public @Override <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return catalog.getResource(id, clazz);
    }

    public @Override <T extends ResourceInfo> T getResourceByName(
            String ns, String name, Class<T> clazz) {
        return catalog.getResourceByName(ns, name, clazz);
    }

    public @Override <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo ns, String name, Class<T> clazz) {
        return catalog.getResourceByName(ns, name, clazz);
    }

    public @Override <T extends ResourceInfo> T getResourceByName(Name name, Class<T> clazz) {
        return catalog.getResourceByName(name, clazz);
    }

    public @Override <T extends ResourceInfo> T getResourceByName(String name, Class<T> clazz) {
        return catalog.getResourceByName(name, clazz);
    }

    public @Override void add(ResourceInfo resource) {
        catalog.add(resource);
    }

    public @Override ValidationResult validate(ResourceInfo resource, boolean isNew) {
        return catalog.validate(resource, isNew);
    }

    public @Override void remove(ResourceInfo resource) {
        catalog.remove(resource);
    }

    public @Override void save(ResourceInfo resource) {
        catalog.save(resource);
    }

    public @Override <T extends ResourceInfo> T detach(T resource) {
        return catalog.detach(resource);
    }

    public @Override <T extends StoreInfo> T detach(T store) {
        return catalog.detach(store);
    }

    public @Override <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return catalog.getResources(clazz);
    }

    public @Override <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        return catalog.getResourcesByNamespace(namespace, clazz);
    }

    public @Override <T extends ResourceInfo> List<T> getResourcesByNamespace(
            String namespace, Class<T> clazz) {
        return catalog.getResourcesByNamespace(namespace, clazz);
    }

    public @Override <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        return catalog.getResourceByStore(store, name, clazz);
    }

    public @Override <T extends ResourceInfo> List<T> getResourcesByStore(
            StoreInfo store, Class<T> clazz) {
        return catalog.getResourcesByStore(store, clazz);
    }

    public @Override FeatureTypeInfo getFeatureType(String id) {
        return catalog.getFeatureType(id);
    }

    public @Override FeatureTypeInfo getFeatureTypeByName(String ns, String name) {
        return catalog.getFeatureTypeByName(ns, name);
    }

    public @Override FeatureTypeInfo getFeatureTypeByName(NamespaceInfo ns, String name) {
        return catalog.getFeatureTypeByName(ns, name);
    }

    public @Override FeatureTypeInfo getFeatureTypeByName(Name name) {
        return catalog.getFeatureTypeByName(name);
    }

    public @Override FeatureTypeInfo getFeatureTypeByName(String name) {
        return catalog.getFeatureTypeByName(name);
    }

    public @Override List<FeatureTypeInfo> getFeatureTypes() {
        return catalog.getFeatureTypes();
    }

    public @Override List<FeatureTypeInfo> getFeatureTypesByNamespace(NamespaceInfo namespace) {
        return catalog.getFeatureTypesByNamespace(namespace);
    }

    public @Override FeatureTypeInfo getFeatureTypeByDataStore(
            DataStoreInfo dataStore, String name) {
        return catalog.getFeatureTypeByDataStore(dataStore, name);
    }

    public @Override List<FeatureTypeInfo> getFeatureTypesByDataStore(DataStoreInfo store) {
        return catalog.getFeatureTypesByDataStore(store);
    }

    public @Override CoverageInfo getCoverage(String id) {
        return catalog.getCoverage(id);
    }

    public @Override CoverageInfo getCoverageByName(String ns, String name) {
        return catalog.getCoverageByName(ns, name);
    }

    public @Override CoverageInfo getCoverageByName(NamespaceInfo ns, String name) {
        return catalog.getCoverageByName(ns, name);
    }

    public @Override CoverageInfo getCoverageByName(Name name) {
        return catalog.getCoverageByName(name);
    }

    public @Override CoverageInfo getCoverageByName(String name) {
        return catalog.getCoverageByName(name);
    }

    public @Override List<CoverageInfo> getCoverages() {
        return catalog.getCoverages();
    }

    public @Override List<CoverageInfo> getCoveragesByNamespace(NamespaceInfo namespace) {
        return catalog.getCoveragesByNamespace(namespace);
    }

    public @Override CoverageInfo getCoverageByCoverageStore(
            CoverageStoreInfo coverageStore, String name) {
        return catalog.getCoverageByCoverageStore(coverageStore, name);
    }

    public @Override List<CoverageInfo> getCoveragesByCoverageStore(CoverageStoreInfo store) {
        return catalog.getCoveragesByCoverageStore(store);
    }

    public @Override void add(LayerInfo layer) {
        catalog.add(layer);
    }

    public @Override ValidationResult validate(LayerInfo layer, boolean isNew) {
        return catalog.validate(layer, isNew);
    }

    public @Override void remove(LayerInfo layer) {
        catalog.remove(layer);
    }

    public @Override void save(LayerInfo layer) {
        catalog.save(layer);
    }

    public @Override LayerInfo detach(LayerInfo layer) {
        return catalog.detach(layer);
    }

    public @Override List<CoverageInfo> getCoveragesByStore(CoverageStoreInfo store) {
        return catalog.getCoveragesByStore(store);
    }

    public @Override LayerInfo getLayer(String id) {
        return catalog.getLayer(id);
    }

    public @Override LayerInfo getLayerByName(String name) {
        return catalog.getLayerByName(name);
    }

    public @Override LayerInfo getLayerByName(Name name) {
        return catalog.getLayerByName(name);
    }

    public @Override List<LayerInfo> getLayers() {
        return catalog.getLayers();
    }

    public @Override List<LayerInfo> getLayers(ResourceInfo resource) {
        return catalog.getLayers(resource);
    }

    public @Override List<LayerInfo> getLayers(StyleInfo style) {
        return catalog.getLayers(style);
    }

    public @Override void add(MapInfo map) {
        catalog.add(map);
    }

    public @Override void remove(MapInfo map) {
        catalog.remove(map);
    }

    public @Override void save(MapInfo map) {
        catalog.save(map);
    }

    public @Override MapInfo detach(MapInfo map) {
        return catalog.detach(map);
    }

    public @Override List<MapInfo> getMaps() {
        return catalog.getMaps();
    }

    public @Override MapInfo getMap(String id) {
        return catalog.getMap(id);
    }

    public @Override MapInfo getMapByName(String name) {
        return catalog.getMapByName(name);
    }

    public @Override void add(LayerGroupInfo layerGroup) {
        catalog.add(layerGroup);
    }

    public @Override ValidationResult validate(LayerGroupInfo layerGroup, boolean isNew) {
        return catalog.validate(layerGroup, isNew);
    }

    public @Override void remove(LayerGroupInfo layerGroup) {
        catalog.remove(layerGroup);
    }

    public @Override void save(LayerGroupInfo layerGroup) {
        catalog.save(layerGroup);
    }

    public @Override LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return catalog.detach(layerGroup);
    }

    public @Override List<LayerGroupInfo> getLayerGroups() {
        return catalog.getLayerGroups();
    }

    public @Override List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName) {
        return catalog.getLayerGroupsByWorkspace(workspaceName);
    }

    public @Override List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return catalog.getLayerGroupsByWorkspace(workspace);
    }

    public @Override LayerGroupInfo getLayerGroup(String id) {
        return catalog.getLayerGroup(id);
    }

    public @Override LayerGroupInfo getLayerGroupByName(String name) {
        return catalog.getLayerGroupByName(name);
    }

    public @Override LayerGroupInfo getLayerGroupByName(String workspaceName, String name) {
        return catalog.getLayerGroupByName(workspaceName, name);
    }

    public @Override LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return catalog.getLayerGroupByName(workspace, name);
    }

    public @Override void add(StyleInfo style) {
        catalog.add(style);
    }

    public @Override ValidationResult validate(StyleInfo style, boolean isNew) {
        return catalog.validate(style, isNew);
    }

    public @Override void remove(StyleInfo style) {
        catalog.remove(style);
    }

    public @Override void save(StyleInfo style) {
        catalog.save(style);
    }

    public @Override StyleInfo detach(StyleInfo style) {
        return catalog.detach(style);
    }

    public @Override StyleInfo getStyle(String id) {
        return catalog.getStyle(id);
    }

    public @Override StyleInfo getStyleByName(String workspaceName, String name) {
        return catalog.getStyleByName(workspaceName, name);
    }

    public @Override StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return catalog.getStyleByName(workspace, name);
    }

    public @Override StyleInfo getStyleByName(String name) {
        return catalog.getStyleByName(name);
    }

    public @Override List<StyleInfo> getStyles() {
        return catalog.getStyles();
    }

    public @Override List<StyleInfo> getStylesByWorkspace(String workspaceName) {
        return catalog.getStylesByWorkspace(workspaceName);
    }

    public @Override List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return catalog.getStylesByWorkspace(workspace);
    }

    public @Override void add(NamespaceInfo namespace) {
        catalog.add(namespace);
    }

    public @Override ValidationResult validate(NamespaceInfo namespace, boolean isNew) {
        return catalog.validate(namespace, isNew);
    }

    public @Override void remove(NamespaceInfo namespace) {
        catalog.remove(namespace);
    }

    public @Override void save(NamespaceInfo namespace) {
        catalog.save(namespace);
    }

    public @Override NamespaceInfo detach(NamespaceInfo namespace) {
        return catalog.detach(namespace);
    }

    public @Override NamespaceInfo getNamespace(String id) {
        return catalog.getNamespace(id);
    }

    public @Override NamespaceInfo getNamespaceByPrefix(String prefix) {
        return catalog.getNamespaceByPrefix(prefix);
    }

    public @Override NamespaceInfo getNamespaceByURI(String uri) {
        return catalog.getNamespaceByURI(uri);
    }

    public @Override NamespaceInfo getDefaultNamespace() {
        return catalog.getDefaultNamespace();
    }

    public @Override void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        catalog.setDefaultNamespace(defaultNamespace);
    }

    public @Override List<NamespaceInfo> getNamespaces() {
        return catalog.getNamespaces();
    }

    public @Override void add(WorkspaceInfo workspace) {
        catalog.add(workspace);
    }

    public @Override ValidationResult validate(WorkspaceInfo workspace, boolean isNew) {
        return catalog.validate(workspace, isNew);
    }

    public @Override void remove(WorkspaceInfo workspace) {
        catalog.remove(workspace);
    }

    public @Override void save(WorkspaceInfo workspace) {
        catalog.save(workspace);
    }

    public @Override WorkspaceInfo detach(WorkspaceInfo workspace) {
        return catalog.detach(workspace);
    }

    public @Override WorkspaceInfo getDefaultWorkspace() {
        return catalog.getDefaultWorkspace();
    }

    public @Override void setDefaultWorkspace(WorkspaceInfo workspace) {
        catalog.setDefaultWorkspace(workspace);
    }

    public @Override List<WorkspaceInfo> getWorkspaces() {
        return catalog.getWorkspaces();
    }

    public @Override WorkspaceInfo getWorkspace(String id) {
        return catalog.getWorkspace(id);
    }

    public @Override WorkspaceInfo getWorkspaceByName(String name) {
        return catalog.getWorkspaceByName(name);
    }

    public @Override Collection<CatalogListener> getListeners() {
        return catalog.getListeners();
    }

    public @Override void addListener(CatalogListener listener) {
        catalog.addListener(listener);
    }

    public @Override void removeListener(CatalogListener listener) {
        catalog.removeListener(listener);
    }

    public @Override void fireAdded(CatalogInfo object) {
        catalog.fireAdded(object);
    }

    public @Override void fireModified(
            CatalogInfo object,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        catalog.fireModified(object, propertyNames, oldValues, newValues);
    }

    public @Override void firePostModified(
            CatalogInfo object,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        catalog.firePostModified(object, propertyNames, oldValues, newValues);
    }

    public @Override void fireRemoved(CatalogInfo object) {
        catalog.fireRemoved(object);
    }

    public @Override ResourcePool getResourcePool() {
        return catalog.getResourcePool();
    }

    public @Override void setResourcePool(ResourcePool resourcePool) {
        catalog.setResourcePool(resourcePool);
    }

    public @Override GeoServerResourceLoader getResourceLoader() {
        return catalog.getResourceLoader();
    }

    public @Override void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        catalog.setResourceLoader(resourceLoader);
    }

    public @Override void dispose() {
        catalog.dispose();
    }

    public @Override <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        return catalog.count(of, filter);
    }

    public @Override <T extends CatalogInfo> T get(Class<T> type, Filter filter)
            throws IllegalArgumentException {
        return catalog.get(type, filter);
    }

    public @Override <T extends CatalogInfo> CloseableIterator<T> list(Class<T> of, Filter filter) {
        return catalog.list(of, filter);
    }

    public @Override <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of, Filter filter, Integer offset, Integer count, SortBy sortBy) {
        return catalog.list(of, filter, offset, count, sortBy);
    }

    public @Override void removeListeners(@SuppressWarnings("rawtypes") Class listenerClass) {
        catalog.removeListeners(listenerClass);
    }

    public @Override CatalogCapabilities getCatalogCapabilities() {
        return catalog.getCatalogCapabilities();
    }
}
