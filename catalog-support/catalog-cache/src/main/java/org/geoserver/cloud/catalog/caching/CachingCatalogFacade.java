/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.forwarding.ForwardingExtendedCatalogFacade;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

/** */
@CacheConfig(cacheNames = {"catalog"})
public class CachingCatalogFacade extends ForwardingExtendedCatalogFacade {

    public CachingCatalogFacade(ExtendedCatalogFacade facade) {
        super(facade);
    }

    public @Override @CachePut(key = "#p0.id") StoreInfo add(StoreInfo store) {
        return super.add(store);
    }

    public @Override @CachePut(key = "#p0.id") ResourceInfo add(ResourceInfo resource) {
        return super.add(resource);
    }

    public @Override @CachePut(key = "#p0.id") LayerInfo add(LayerInfo layer) {
        return super.add(layer);
    }

    public @Override @CachePut(key = "#p0.id") LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return super.add(layerGroup);
    }

    public @Override @CachePut(key = "#p0.id") NamespaceInfo add(NamespaceInfo namespace) {
        return super.add(namespace);
    }

    public @Override @CachePut(key = "#p0.id") WorkspaceInfo add(WorkspaceInfo workspace) {
        return super.add(workspace);
    }

    public @Override @CachePut(key = "#p0.id") StyleInfo add(StyleInfo style) {
        return super.add(style);
    }

    public @Override @CacheEvict(key = "#p0.id") void remove(StoreInfo store) {
        super.remove(store);
    }

    public @Override @CacheEvict(key = "#p0.id") void remove(ResourceInfo resource) {
        super.remove(resource);
    }

    public @Override @CacheEvict(key = "#p0.id") void remove(LayerInfo layer) {
        super.remove(layer);
    }

    public @Override @CacheEvict(key = "#p0.id") void remove(LayerGroupInfo layerGroup) {
        super.remove(layerGroup);
    }

    public @Override @CacheEvict(key = "#p0.id") void remove(NamespaceInfo namespace) {
        super.remove(namespace);
    }

    public @Override @CacheEvict(key = "#p0.id") void remove(WorkspaceInfo workspace) {
        super.remove(workspace);
    }

    public @Override @CacheEvict(key = "#p0.id") void remove(StyleInfo style) {
        super.remove(style);
    }

    public @Override @CacheEvict(key = "#p0.id") void save(StoreInfo store) {
        super.remove(store);
    }

    public @Override @CacheEvict(key = "#p0.id") void save(ResourceInfo resource) {
        super.remove(resource);
    }

    public @Override @CacheEvict(key = "#p0.id") void save(StyleInfo style) {
        super.save(style);
    }

    public @Override @CacheEvict(key = "#p0.id") void save(LayerInfo layer) {
        super.save(layer);
    }

    public @Override @CacheEvict(key = "#p0.id") void save(LayerGroupInfo layerGroup) {
        super.save(layerGroup);
    }

    public @Override @CacheEvict(key = "#p0.id") void save(NamespaceInfo namespace) {
        super.save(namespace);
    }

    public @Override @CacheEvict(key = "#p0.id") void save(WorkspaceInfo workspace) {
        super.save(workspace);
    }

    public @Override @CacheEvict(key = "#p0.id") <I extends CatalogInfo> I update(
            final I info, final Patch patch) {
        return super.update(info, patch);
    }

    public @Override @Cacheable WorkspaceInfo getWorkspace(String id) {
        return super.getWorkspace(id);
    }

    public @Override @Cacheable NamespaceInfo getNamespace(String id) {
        return super.getNamespace(id);
    }

    @Cacheable(key = "#p0")
    public @Override <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return super.getStore(id, clazz);
    }

    @Cacheable(key = "#p0")
    public @Override <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return super.getResource(id, clazz);
    }

    public @Override @Cacheable StyleInfo getStyle(String id) {
        return super.getStyle(id);
    }

    public @Override @Cacheable LayerInfo getLayer(String id) {
        return super.getLayer(id);
    }

    public @Override @Cacheable LayerGroupInfo getLayerGroup(String id) {
        return super.getLayerGroup(id);
    }

    @Cacheable(key = "'defaultWorkspace'")
    public @Override WorkspaceInfo getDefaultWorkspace() {
        return super.getDefaultWorkspace();
    }

    @CachePut(key = "'defaultWorkspace'")
    public @Override void setDefaultWorkspace(WorkspaceInfo workspace) {
        super.setDefaultWorkspace(workspace);
    }

    @Cacheable(key = "'defaultNamespace'")
    public @Override NamespaceInfo getDefaultNamespace() {
        return super.getDefaultNamespace();
    }

    @CachePut(key = "'defaultNamespace'")
    public @Override void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        super.setDefaultNamespace(defaultNamespace);
    }

    @Cacheable(key = "'defaultDataStore.' + #p0.id")
    public @Override DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return super.getDefaultDataStore(workspace);
    }

    @CacheEvict(key = "'defaultDataStore.' + #p0.id")
    public @Override void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        super.setDefaultDataStore(workspace, store);
    }

    // public @Override <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace, String
    // name, Class<T> clazz){}
    // public @Override LayerInfo getLayerByName(String name){}
    // public @Override LayerGroupInfo getLayerGroupByName(String name){}
    // public @Override LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String
    // name){}
    // public @Override NamespaceInfo getNamespaceByPrefix(String prefix){}
    // public @Override NamespaceInfo getNamespaceByURI(String uri){}
    // public @Override WorkspaceInfo getWorkspaceByName(String name){}
    // public @Override StyleInfo getStyleByName(String name){}
    // public @Override StyleInfo getStyleByName(WorkspaceInfo workspace, String name){}
}
