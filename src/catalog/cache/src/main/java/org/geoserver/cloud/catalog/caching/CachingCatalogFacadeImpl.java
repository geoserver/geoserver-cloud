/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

import static org.geoserver.cloud.catalog.caching.CachingCatalogFacade.generateLayersByResourceKey;

import lombok.NonNull;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.util.List;
import java.util.function.BiFunction;

/** */
@CacheConfig(cacheNames = {CachingCatalogFacade.CACHE_NAME})
public class CachingCatalogFacadeImpl extends ForwardingExtendedCatalogFacade
        implements CachingCatalogFacade {
    private Cache idCache;

    public CachingCatalogFacadeImpl(ExtendedCatalogFacade facade) {
        super(facade);
    }

    public @Autowired void setCacheManager(CacheManager cacheManager) {
        idCache = cacheManager.getCache(CachingCatalogFacade.CACHE_NAME);
    }

    public @Override boolean evict(CatalogInfo info) {
        if (info == null || idCache == null) return false;

        if (info instanceof ResourceInfo) {
            CatalogInfoKey layersByResourceKey = generateLayersByResourceKey((ResourceInfo) info);
            idCache.evict(layersByResourceKey);
        } else if (info instanceof LayerInfo) {
            LayerInfo l = (LayerInfo) info;
            ResourceInfo r = l.getResource();
            if (r != null) {
                CatalogInfoKey layersByResourceKey = generateLayersByResourceKey(r);
                idCache.evict(layersByResourceKey);
            }
        }
        CatalogInfoKey key = new CatalogInfoKey(info);
        boolean evicted = idCache.evictIfPresent(key);
        return evicted;
    }

    public @Override boolean evict(@NonNull Object key) {
        return idCache.evictIfPresent(key);
    }

    @CachePut(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override StoreInfo add(StoreInfo store) {
        return super.add(store);
    }

    @CachePut(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override ResourceInfo add(ResourceInfo resource) {
        return super.add(resource);
    }

    @CachePut(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override LayerInfo add(LayerInfo layer) {
        return super.add(layer);
    }

    @CachePut(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return super.add(layerGroup);
    }

    @CachePut(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override NamespaceInfo add(NamespaceInfo namespace) {
        return super.add(namespace);
    }

    @CachePut(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override WorkspaceInfo add(WorkspaceInfo workspace) {
        return super.add(workspace);
    }

    @CachePut(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override StyleInfo add(StyleInfo style) {
        return super.add(style);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override void remove(StoreInfo store) {
        super.remove(store);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override void remove(ResourceInfo resource) {
        super.remove(resource);
    }

    @Caching(
            evict = {
                // cached layers
                @CacheEvict(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)"),
                // layers by resource (see getLayers(ResourceInfo)
                @CacheEvict(
                        key =
                                "new org.geoserver.cloud.catalog.caching.CatalogInfoKey('layers@' + #layer.resource.id, 'LAYER')")
            })
    public @Override void remove(LayerInfo layer) {
        super.remove(layer);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override void remove(LayerGroupInfo layerGroup) {
        super.remove(layerGroup);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override void remove(NamespaceInfo namespace) {
        super.remove(namespace);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override void remove(WorkspaceInfo workspace) {
        super.remove(workspace);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override void remove(StyleInfo style) {
        super.remove(style);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override void save(StoreInfo store) {
        super.remove(store);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override void save(ResourceInfo resource) {
        super.remove(resource);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override void save(StyleInfo style) {
        super.save(style);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override void save(LayerInfo layer) {
        super.save(layer);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override void save(LayerGroupInfo layerGroup) {
        super.save(layerGroup);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override void save(NamespaceInfo namespace) {
        super.save(namespace);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#p0)")
    public @Override void save(WorkspaceInfo workspace) {
        super.save(workspace);
    }

    @CachePut(
            key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#info)",
            unless = "#result == null")
    public @Override <I extends CatalogInfo> I update(final I info, final Patch patch) {
        return super.update(info, patch);
    }

    @Cacheable(
            key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#id, 'WORKSPACE')",
            unless = "#result == null")
    public @Override WorkspaceInfo getWorkspace(String id) {
        return super.getWorkspace(id);
    }

    @Cacheable(
            key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#id, 'NAMESPACE')",
            unless = "#result == null")
    public @Override NamespaceInfo getNamespace(String id) {
        return super.getNamespace(id);
    }

    /**
     * @implNote manual caching; checks the cache using the requested type, but caches using the
     *     result's concrete type
     */
    public @Override <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return getOrCache(id, clazz, super::getStore);
    }

    /**
     * @implNote manual caching; checks the cache using the requested type, but caches using the
     *     result's concrete type
     */
    public @Override <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return getOrCache(id, clazz, super::getResource);
    }

    /**
     * Caching query by id and (possibly abstract) type; performs manual caching accessing the
     * {@link #idCache} directly to check for cache hit using the requested type, but storing a
     * cache entry using the concrete result type
     */
    private <T extends CatalogInfo> T getOrCache(
            String id, Class<T> requestType, BiFunction<String, Class<T>, T> queryMethod) {
        // will be non-null if there's an entry and requestType is either a base type (e.g.
        // ResourceInfo) or the concrete type (e.g. DataStoreInfo), due to the smarts in
        // Key.equals()
        ValueWrapper value = idCache.get(new CatalogInfoKey(id, requestType));
        if (value != null) {
            CatalogInfo info = (CatalogInfo) value.get();
            return requestType.isInstance(info) ? requestType.cast(info) : null;
        }
        // on cache miss, put it with a key using the concrete type
        T result = queryMethod.apply(id, requestType);
        if (result != null) {
            idCache.putIfAbsent(new CatalogInfoKey(result), result);
        }
        return result;
    }

    @Cacheable(
            key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#id, 'STYLE')",
            unless = "#result == null")
    public @Override StyleInfo getStyle(String id) {
        return super.getStyle(id);
    }

    @Cacheable(
            key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#id, 'LAYER')",
            unless = "#result == null")
    public @Override LayerInfo getLayer(String id) {
        return super.getLayer(id);
    }

    @Cacheable(
            key =
                    "new org.geoserver.cloud.catalog.caching.CatalogInfoKey('layers@' + #resource.id, 'LAYER')",
            unless = "#result == null")
    public @Override List<LayerInfo> getLayers(ResourceInfo resource) {
        return super.getLayers(resource);
    }

    @Cacheable(
            key = "new org.geoserver.cloud.catalog.caching.CatalogInfoKey(#id, 'LAYERGROUP')",
            unless = "#result == null")
    public @Override LayerGroupInfo getLayerGroup(String id) {
        return super.getLayerGroup(id);
    }

    @Cacheable(key = "'" + DEFAULT_WORKSPACE_CACHE_KEY + "'", unless = "#result == null")
    public @Override WorkspaceInfo getDefaultWorkspace() {
        return super.getDefaultWorkspace();
    }

    @CacheEvict(key = "'" + DEFAULT_WORKSPACE_CACHE_KEY + "'")
    public @Override void setDefaultWorkspace(WorkspaceInfo workspace) {
        super.setDefaultWorkspace(workspace);
    }

    @Cacheable(key = "'" + DEFAULT_NAMESPACE_CACHE_KEY + "'", unless = "#result == null")
    public @Override NamespaceInfo getDefaultNamespace() {
        return super.getDefaultNamespace();
    }

    @CacheEvict(key = "'" + DEFAULT_NAMESPACE_CACHE_KEY + "'")
    public @Override void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        super.setDefaultNamespace(defaultNamespace);
    }

    @Cacheable(
            key = "'" + DEFAULT_DATASTORE_CACHE_KEY_PREFIX + "' + #p0.id",
            unless = "#result == null")
    public @Override DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return super.getDefaultDataStore(workspace);
    }

    @CacheEvict(key = "'" + DEFAULT_DATASTORE_CACHE_KEY_PREFIX + "' + #p0.id")
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
