/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.List;
import javax.annotation.Nullable;
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
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * {@link CatalogFacade} which forwards all its method calls to another {@code CatalogFacade} aiding
 * in implementing a decorator.
 *
 * <p>Subclasses should override one or more methods to modify the behavior of the backing facade as
 * needed.
 */
public class ForwardingCatalogFacade implements CatalogFacade {

    // wrapped catalog facade
    private final CatalogFacade facade;

    public ForwardingCatalogFacade(CatalogFacade facade) {
        this.facade = facade;
    }

    public @Override Catalog getCatalog() {
        return facade.getCatalog();
    }

    public @Override void setCatalog(Catalog catalog) {
        facade.setCatalog(catalog);
    }

    public @Override StoreInfo add(StoreInfo store) {
        return facade.add(store);
    }

    public @Override void remove(StoreInfo store) {
        facade.remove(store);
    }

    public @Override void save(StoreInfo store) {
        facade.save(store);
    }

    public @Override <T extends StoreInfo> T detach(T store) {
        return facade.detach(store);
    }

    public @Override <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return facade.getStore(id, clazz);
    }

    public @Override <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {
        return facade.getStoreByName(workspace, name, clazz);
    }

    public @Override <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        return facade.getStoresByWorkspace(workspace, clazz);
    }

    public @Override <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return facade.getStores(clazz);
    }

    public @Override DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return facade.getDefaultDataStore(workspace);
    }

    public @Override void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        facade.setDefaultDataStore(workspace, store);
    }

    public @Override ResourceInfo add(ResourceInfo resource) {
        return facade.add(resource);
    }

    public @Override void remove(ResourceInfo resource) {
        facade.remove(resource);
    }

    public @Override void save(ResourceInfo resource) {
        facade.save(resource);
    }

    public @Override <T extends ResourceInfo> T detach(T resource) {
        return facade.detach(resource);
    }

    public @Override <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return facade.getResource(id, clazz);
    }

    public @Override <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo namespace, String name, Class<T> clazz) {
        return facade.getResourceByName(namespace, name, clazz);
    }

    public @Override <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return facade.getResources(clazz);
    }

    public @Override <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        return facade.getResourcesByNamespace(namespace, clazz);
    }

    public @Override <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        return facade.getResourceByStore(store, name, clazz);
    }

    public @Override <T extends ResourceInfo> List<T> getResourcesByStore(
            StoreInfo store, Class<T> clazz) {
        return facade.getResourcesByStore(store, clazz);
    }

    public @Override LayerInfo add(LayerInfo layer) {
        return facade.add(layer);
    }

    public @Override void remove(LayerInfo layer) {
        facade.remove(layer);
    }

    public @Override void save(LayerInfo layer) {
        facade.save(layer);
    }

    public @Override LayerInfo detach(LayerInfo layer) {
        return facade.detach(layer);
    }

    public @Override LayerInfo getLayer(String id) {
        return facade.getLayer(id);
    }

    public @Override LayerInfo getLayerByName(String name) {
        return facade.getLayerByName(name);
    }

    public @Override List<LayerInfo> getLayers(ResourceInfo resource) {
        return facade.getLayers(resource);
    }

    public @Override List<LayerInfo> getLayers(StyleInfo style) {
        return facade.getLayers(style);
    }

    public @Override List<LayerInfo> getLayers() {
        return facade.getLayers();
    }

    public @Override MapInfo add(MapInfo map) {
        return facade.add(map);
    }

    public @Override void remove(MapInfo map) {
        facade.remove(map);
    }

    public @Override void save(MapInfo map) {
        facade.save(map);
    }

    public @Override MapInfo detach(MapInfo map) {
        return facade.detach(map);
    }

    public @Override MapInfo getMap(String id) {
        return facade.getMap(id);
    }

    public @Override MapInfo getMapByName(String name) {
        return facade.getMapByName(name);
    }

    public @Override List<MapInfo> getMaps() {
        return facade.getMaps();
    }

    public @Override LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return facade.add(layerGroup);
    }

    public @Override void remove(LayerGroupInfo layerGroup) {
        facade.remove(layerGroup);
    }

    public @Override void save(LayerGroupInfo layerGroup) {
        facade.save(layerGroup);
    }

    public @Override LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return facade.detach(layerGroup);
    }

    public @Override LayerGroupInfo getLayerGroup(String id) {
        return facade.getLayerGroup(id);
    }

    public @Override LayerGroupInfo getLayerGroupByName(String name) {
        return facade.getLayerGroupByName(name);
    }

    public @Override LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return facade.getLayerGroupByName(workspace, name);
    }

    public @Override List<LayerGroupInfo> getLayerGroups() {
        return facade.getLayerGroups();
    }

    public @Override List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return facade.getLayerGroupsByWorkspace(workspace);
    }

    public @Override NamespaceInfo add(NamespaceInfo namespace) {
        return facade.add(namespace);
    }

    public @Override void remove(NamespaceInfo namespace) {
        facade.remove(namespace);
    }

    public @Override void save(NamespaceInfo namespace) {
        facade.save(namespace);
    }

    public @Override NamespaceInfo detach(NamespaceInfo namespace) {
        return facade.detach(namespace);
    }

    public @Override NamespaceInfo getDefaultNamespace() {
        return facade.getDefaultNamespace();
    }

    public @Override void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        facade.setDefaultNamespace(defaultNamespace);
    }

    public @Override NamespaceInfo getNamespace(String id) {
        return facade.getNamespace(id);
    }

    public @Override NamespaceInfo getNamespaceByPrefix(String prefix) {
        return facade.getNamespaceByPrefix(prefix);
    }

    public @Override NamespaceInfo getNamespaceByURI(String uri) {
        return facade.getNamespaceByURI(uri);
    }

    public @Override List<NamespaceInfo> getNamespacesByURI(String uri) {
        return facade.getNamespacesByURI(uri);
    }

    public @Override List<NamespaceInfo> getNamespaces() {
        return facade.getNamespaces();
    }

    public @Override WorkspaceInfo add(WorkspaceInfo workspace) {
        return facade.add(workspace);
    }

    public @Override void remove(WorkspaceInfo workspace) {
        facade.remove(workspace);
    }

    public @Override void save(WorkspaceInfo workspace) {
        facade.save(workspace);
    }

    public @Override WorkspaceInfo detach(WorkspaceInfo workspace) {
        return facade.detach(workspace);
    }

    public @Override WorkspaceInfo getDefaultWorkspace() {
        return facade.getDefaultWorkspace();
    }

    public @Override void setDefaultWorkspace(WorkspaceInfo workspace) {
        facade.setDefaultWorkspace(workspace);
    }

    public @Override WorkspaceInfo getWorkspace(String id) {
        return facade.getWorkspace(id);
    }

    public @Override WorkspaceInfo getWorkspaceByName(String name) {
        return facade.getWorkspaceByName(name);
    }

    public @Override List<WorkspaceInfo> getWorkspaces() {
        return facade.getWorkspaces();
    }

    public @Override StyleInfo add(StyleInfo style) {
        return facade.add(style);
    }

    public @Override void remove(StyleInfo style) {
        facade.remove(style);
    }

    public @Override void save(StyleInfo style) {
        facade.save(style);
    }

    public @Override StyleInfo detach(StyleInfo style) {
        return facade.detach(style);
    }

    public @Override StyleInfo getStyle(String id) {
        return facade.getStyle(id);
    }

    public @Override StyleInfo getStyleByName(String name) {
        return facade.getStyleByName(name);
    }

    public @Override StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return facade.getStyleByName(workspace, name);
    }

    public @Override List<StyleInfo> getStyles() {
        return facade.getStyles();
    }

    public @Override List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return facade.getStylesByWorkspace(workspace);
    }

    public @Override void dispose() {
        facade.dispose();
    }

    public @Override void resolve() {
        facade.resolve();
    }

    public @Override void syncTo(CatalogFacade other) {
        facade.syncTo(other);
    }

    public @Override <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        return facade.count(of, filter);
    }

    public @Override boolean canSort(Class<? extends CatalogInfo> type, String propertyName) {
        return facade.canSort(type, propertyName);
    }

    public @Override <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of,
            Filter filter,
            @Nullable Integer offset,
            @Nullable Integer count,
            @Nullable SortBy... sortOrder) {
        return facade.list(of, filter, offset, count, sortOrder);
    }

    public @Override CatalogCapabilities getCatalogCapabilities() {
        CatalogCapabilities capabilities = facade.getCatalogCapabilities();
        // this wrapper adds support for isolated workspaces
        capabilities.setIsolatedWorkspacesSupport(true);
        return capabilities;
    }
}
