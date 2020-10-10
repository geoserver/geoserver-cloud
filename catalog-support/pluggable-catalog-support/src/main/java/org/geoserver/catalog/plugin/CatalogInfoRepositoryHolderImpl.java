/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;

public class CatalogInfoRepositoryHolderImpl implements CatalogInfoRepositoryHolder {

    protected NamespaceRepository namespaces;
    protected WorkspaceRepository workspaces;
    protected StoreRepository stores;
    protected ResourceRepository resources;
    protected LayerRepository layers;
    protected LayerGroupRepository layerGroups;
    protected StyleRepository styles;
    protected MapRepository maps;

    protected CatalogInfoTypeRegistry<CatalogInfoRepository<?>> repos =
            new CatalogInfoTypeRegistry<>();

    @SuppressWarnings("unchecked")
    public @Override <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repository(
            Class<T> of) {
        return (R) repos.of(of);
    }

    @SuppressWarnings("unchecked")
    public @Override <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repositoryFor(
            T info) {
        return (R) repos.forObject(info);
    }

    public @Override void setNamespaceRepository(NamespaceRepository namespaces) {
        this.namespaces = namespaces;
        repos.register(NamespaceInfo.class, namespaces);
    }

    public @Override NamespaceRepository getNamespaceRepository() {
        return namespaces;
    }

    public @Override void setWorkspaceRepository(WorkspaceRepository workspaces) {
        this.workspaces = workspaces;
        repos.register(WorkspaceInfo.class, workspaces);
    }

    public @Override WorkspaceRepository getWorkspaceRepository() {
        return workspaces;
    }

    public @Override void setStoreRepository(StoreRepository stores) {
        this.stores = stores;
        repos.registerRecursively(StoreInfo.class, stores);
    }

    public @Override StoreRepository getStoreRepository() {
        return stores;
    }

    public @Override void setResourceRepository(ResourceRepository resources) {
        this.resources = resources;
        repos.registerRecursively(ResourceInfo.class, resources);
    }

    public @Override ResourceRepository getResourceRepository() {
        return resources;
    }

    public @Override void setLayerRepository(LayerRepository layers) {
        this.layers = layers;
        repos.register(LayerInfo.class, layers);
    }

    public @Override LayerRepository getLayerRepository() {
        return layers;
    }

    public @Override void setLayerGroupRepository(LayerGroupRepository layerGroups) {
        this.layerGroups = layerGroups;
        repos.register(LayerGroupInfo.class, layerGroups);
    }

    public @Override LayerGroupRepository getLayerGroupRepository() {
        return layerGroups;
    }

    public @Override void setStyleRepository(StyleRepository styles) {
        this.styles = styles;
        repos.register(StyleInfo.class, styles);
    }

    public @Override StyleRepository getStyleRepository() {
        return styles;
    }

    public @Override void setMapRepository(MapRepository maps) {
        this.maps = maps;
        repos.register(MapInfo.class, maps);
    }

    public @Override MapRepository getMapRepository() {
        return maps;
    }
}
