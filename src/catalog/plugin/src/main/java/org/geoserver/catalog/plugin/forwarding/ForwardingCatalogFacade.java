/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

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

import java.util.List;

import javax.annotation.Nullable;

/**
 * {@link CatalogFacade} which forwards all its method calls to another {@code CatalogFacade} aiding
 * in implementing a decorator.
 *
 * <p>Subclasses should override one or more methods to modify the behavior of the backing facade as
 * needed.
 */
public class ForwardingCatalogFacade implements CatalogFacade {

    // wrapped catalog facade
    protected final CatalogFacade facade;

    public ForwardingCatalogFacade(CatalogFacade facade) {
        this.facade = facade;
    }

    /**
     * @return this decorator's subject
     */
    public CatalogFacade getSubject() {
        // if you're wondering, I refuse to derive from org.geotools.util.decorate.AbstractDecorator
        // and by extension from java.sql.Wrapper
        return facade;
    }

    @Override
    public Catalog getCatalog() {
        return facade.getCatalog();
    }

    @Override
    public void setCatalog(Catalog catalog) {
        facade.setCatalog(catalog);
    }

    @Override
    public StoreInfo add(StoreInfo store) {
        return facade.add(store);
    }

    @Override
    public void remove(StoreInfo store) {
        facade.remove(store);
    }

    @Override
    public void save(StoreInfo store) {
        facade.save(store);
    }

    @Override
    public <T extends StoreInfo> T detach(T store) {
        return facade.detach(store);
    }

    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return facade.getStore(id, clazz);
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {
        return facade.getStoreByName(workspace, name, clazz);
    }

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        return facade.getStoresByWorkspace(workspace, clazz);
    }

    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return facade.getStores(clazz);
    }

    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return facade.getDefaultDataStore(workspace);
    }

    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        facade.setDefaultDataStore(workspace, store);
    }

    @Override
    public ResourceInfo add(ResourceInfo resource) {
        return facade.add(resource);
    }

    @Override
    public void remove(ResourceInfo resource) {
        facade.remove(resource);
    }

    @Override
    public void save(ResourceInfo resource) {
        facade.save(resource);
    }

    @Override
    public <T extends ResourceInfo> T detach(T resource) {
        return facade.detach(resource);
    }

    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return facade.getResource(id, clazz);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo namespace, String name, Class<T> clazz) {
        return facade.getResourceByName(namespace, name, clazz);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return facade.getResources(clazz);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        return facade.getResourcesByNamespace(namespace, clazz);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        return facade.getResourceByStore(store, name, clazz);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return facade.getResourcesByStore(store, clazz);
    }

    @Override
    public LayerInfo add(LayerInfo layer) {
        return facade.add(layer);
    }

    @Override
    public void remove(LayerInfo layer) {
        facade.remove(layer);
    }

    @Override
    public void save(LayerInfo layer) {
        facade.save(layer);
    }

    @Override
    public LayerInfo detach(LayerInfo layer) {
        return facade.detach(layer);
    }

    @Override
    public LayerInfo getLayer(String id) {
        return facade.getLayer(id);
    }

    @Override
    public LayerInfo getLayerByName(String name) {
        return facade.getLayerByName(name);
    }

    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return facade.getLayers(resource);
    }

    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {
        return facade.getLayers(style);
    }

    @Override
    public List<LayerInfo> getLayers() {
        return facade.getLayers();
    }

    @Override
    public MapInfo add(MapInfo map) {
        return facade.add(map);
    }

    @Override
    public void remove(MapInfo map) {
        facade.remove(map);
    }

    @Override
    public void save(MapInfo map) {
        facade.save(map);
    }

    @Override
    public MapInfo detach(MapInfo map) {
        return facade.detach(map);
    }

    @Override
    public MapInfo getMap(String id) {
        return facade.getMap(id);
    }

    @Override
    public MapInfo getMapByName(String name) {
        return facade.getMapByName(name);
    }

    @Override
    public List<MapInfo> getMaps() {
        return facade.getMaps();
    }

    @Override
    public LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return facade.add(layerGroup);
    }

    @Override
    public void remove(LayerGroupInfo layerGroup) {
        facade.remove(layerGroup);
    }

    @Override
    public void save(LayerGroupInfo layerGroup) {
        facade.save(layerGroup);
    }

    @Override
    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return facade.detach(layerGroup);
    }

    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return facade.getLayerGroup(id);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return facade.getLayerGroupByName(name);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return facade.getLayerGroupByName(workspace, name);
    }

    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return facade.getLayerGroups();
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return facade.getLayerGroupsByWorkspace(workspace);
    }

    @Override
    public NamespaceInfo add(NamespaceInfo namespace) {
        return facade.add(namespace);
    }

    @Override
    public void remove(NamespaceInfo namespace) {
        facade.remove(namespace);
    }

    @Override
    public void save(NamespaceInfo namespace) {
        facade.save(namespace);
    }

    @Override
    public NamespaceInfo detach(NamespaceInfo namespace) {
        return facade.detach(namespace);
    }

    @Override
    public NamespaceInfo getDefaultNamespace() {
        return facade.getDefaultNamespace();
    }

    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        facade.setDefaultNamespace(defaultNamespace);
    }

    @Override
    public NamespaceInfo getNamespace(String id) {
        return facade.getNamespace(id);
    }

    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return facade.getNamespaceByPrefix(prefix);
    }

    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        return facade.getNamespaceByURI(uri);
    }

    @Override
    public List<NamespaceInfo> getNamespacesByURI(String uri) {
        return facade.getNamespacesByURI(uri);
    }

    @Override
    public List<NamespaceInfo> getNamespaces() {
        return facade.getNamespaces();
    }

    @Override
    public WorkspaceInfo add(WorkspaceInfo workspace) {
        return facade.add(workspace);
    }

    @Override
    public void remove(WorkspaceInfo workspace) {
        facade.remove(workspace);
    }

    @Override
    public void save(WorkspaceInfo workspace) {
        facade.save(workspace);
    }

    @Override
    public WorkspaceInfo detach(WorkspaceInfo workspace) {
        return facade.detach(workspace);
    }

    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return facade.getDefaultWorkspace();
    }

    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        facade.setDefaultWorkspace(workspace);
    }

    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return facade.getWorkspace(id);
    }

    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        return facade.getWorkspaceByName(name);
    }

    @Override
    public List<WorkspaceInfo> getWorkspaces() {
        return facade.getWorkspaces();
    }

    @Override
    public StyleInfo add(StyleInfo style) {
        return facade.add(style);
    }

    @Override
    public void remove(StyleInfo style) {
        facade.remove(style);
    }

    @Override
    public void save(StyleInfo style) {
        facade.save(style);
    }

    @Override
    public StyleInfo detach(StyleInfo style) {
        return facade.detach(style);
    }

    @Override
    public StyleInfo getStyle(String id) {
        return facade.getStyle(id);
    }

    @Override
    public StyleInfo getStyleByName(String name) {
        return facade.getStyleByName(name);
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return facade.getStyleByName(workspace, name);
    }

    @Override
    public List<StyleInfo> getStyles() {
        return facade.getStyles();
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return facade.getStylesByWorkspace(workspace);
    }

    @Override
    public void dispose() {
        facade.dispose();
    }

    @Override
    public void resolve() {
        facade.resolve();
    }

    @Override
    public void syncTo(CatalogFacade other) {
        facade.syncTo(other);
    }

    @Override
    public <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        return facade.count(of, filter);
    }

    @Override
    public boolean canSort(Class<? extends CatalogInfo> type, String propertyName) {
        return facade.canSort(type, propertyName);
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of,
            Filter filter,
            @Nullable Integer offset,
            @Nullable Integer count,
            @Nullable SortBy... sortOrder) {
        return facade.list(of, filter, offset, count, sortOrder);
    }

    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        return facade.getCatalogCapabilities();
    }
}
