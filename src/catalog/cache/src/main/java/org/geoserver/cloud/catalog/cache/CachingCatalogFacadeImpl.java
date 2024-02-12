/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.cache;

import static org.geoserver.cloud.catalog.cache.CachingCatalogFacade.generateLayersByResourceKey;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.forwarding.ForwardingExtendedCatalogFacade;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.util.List;
import java.util.function.BiFunction;

/** */
@CacheConfig(cacheNames = {CachingCatalogFacade.CACHE_NAME})
class CachingCatalogFacadeImpl extends ForwardingExtendedCatalogFacade
        implements CachingCatalogFacade {
    private Cache idCache;

    public CachingCatalogFacadeImpl(@NonNull ExtendedCatalogFacade facade, @NonNull Cache cache) {
        super(facade);
        this.idCache = cache;
    }

    @Override
    public boolean evict(CatalogInfo info) {
        if (info == null || idCache == null) return false;

        if (info instanceof Catalog) {
            boolean evicted = idCache.evictIfPresent(DEFAULT_WORKSPACE_CACHE_KEY);
            evicted |= idCache.evictIfPresent(DEFAULT_NAMESPACE_CACHE_KEY);
            return evicted;
        }

        if (info instanceof ResourceInfo ri) {
            CatalogInfoKey layersByResourceKey = generateLayersByResourceKey(ri);
            idCache.evict(layersByResourceKey);
        } else if (info instanceof LayerInfo l) {
            ResourceInfo r = l.getResource();
            if (r != null) {
                CatalogInfoKey layersByResourceKey = generateLayersByResourceKey(r);
                idCache.evict(layersByResourceKey);
            }
        } else if (info instanceof WorkspaceInfo workspace) {
            idCache.evictIfPresent(CachingCatalogFacade.generateDefaultDataStoreKey(workspace));
        }

        CatalogInfoKey key = new CatalogInfoKey(info);
        return idCache.evictIfPresent(key);
    }

    @Override
    public boolean evict(@NonNull Object key) {
        return idCache.evictIfPresent(key);
    }

    @Override
    @CachePut(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    public <T extends CatalogInfo> T add(@NonNull T info) {
        return super.add(info);
    }

    @Override
    @CacheEvict(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    public void remove(@NonNull CatalogInfo info) {
        super.remove(info);
    }

    @CachePut(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public StoreInfo add(StoreInfo store) {
        return super.add(store);
    }

    @CachePut(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public ResourceInfo add(ResourceInfo resource) {
        return super.add(resource);
    }

    @CachePut(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public LayerInfo add(LayerInfo layer) {
        return super.add(layer);
    }

    @CachePut(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public NamespaceInfo add(NamespaceInfo namespace) {
        return super.add(namespace);
    }

    @CachePut(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public WorkspaceInfo add(WorkspaceInfo workspace) {
        return super.add(workspace);
    }

    @CachePut(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public StyleInfo add(StyleInfo style) {
        return super.add(style);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public void remove(StoreInfo store) {
        super.remove(store);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public void remove(ResourceInfo resource) {
        super.remove(resource);
    }

    @Caching(
            evict = {
                // cached layers
                @CacheEvict(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)"),
                // layers by resource (see getLayers(ResourceInfo)
                @CacheEvict(
                        key =
                                "new org.geoserver.cloud.catalog.cache.CatalogInfoKey('layers@' + #layer.resource.id, 'LAYER')")
            })
    @Override
    public void remove(LayerInfo layer) {
        super.remove(layer);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public void remove(NamespaceInfo namespace) {
        super.remove(namespace);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public void remove(WorkspaceInfo workspace) {
        super.remove(workspace);
    }

    @CacheEvict(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public void remove(StyleInfo style) {
        super.remove(style);
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#save(StoreInfo)} use {@link
     *     #update(CatalogInfo, Patch)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @CacheEvict(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public void save(StoreInfo store) {
        super.remove(store);
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#save(StoreInfo)} use {@link
     *     #update(CatalogInfo, Patch)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @CacheEvict(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public void save(ResourceInfo resource) {
        super.remove(resource);
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#save(StoreInfo)} use {@link
     *     #update(CatalogInfo, Patch)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @CacheEvict(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public void save(StyleInfo style) {
        super.save(style);
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#save(StoreInfo)} use {@link
     *     #update(CatalogInfo, Patch)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @CacheEvict(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public void save(LayerInfo layer) {
        super.save(layer);
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#save(StoreInfo)} use {@link
     *     #update(CatalogInfo, Patch)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @CacheEvict(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public void save(NamespaceInfo namespace) {
        super.save(namespace);
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#save(StoreInfo)} use {@link
     *     #update(CatalogInfo, Patch)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @CacheEvict(key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#p0)")
    @Override
    public void save(WorkspaceInfo workspace) {
        super.save(workspace);
    }

    @CachePut(
            key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#info)",
            unless = "#result == null")
    @Override
    public <I extends CatalogInfo> I update(final I info, final Patch patch) {
        return super.update(info, patch);
    }

    @Cacheable(
            key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#id, 'WORKSPACE')",
            unless = "#result == null")
    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return super.getWorkspace(id);
    }

    @Cacheable(
            key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#id, 'NAMESPACE')",
            unless = "#result == null")
    @Override
    public NamespaceInfo getNamespace(String id) {
        return super.getNamespace(id);
    }

    /**
     * @implNote manual caching; checks the cache using the requested type, but caches using the
     *     result's concrete type
     */
    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return getOrCache(id, clazz, super::getStore);
    }

    /**
     * @implNote manual caching; checks the cache using the requested type, but caches using the
     *     result's concrete type
     */
    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
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
            key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#id, 'STYLE')",
            unless = "#result == null")
    @Override
    public StyleInfo getStyle(String id) {
        return super.getStyle(id);
    }

    @Cacheable(
            key = "new org.geoserver.cloud.catalog.cache.CatalogInfoKey(#id, 'LAYER')",
            unless = "#result == null")
    @Override
    public LayerInfo getLayer(String id) {
        return super.getLayer(id);
    }

    @Cacheable(
            key =
                    "new org.geoserver.cloud.catalog.cache.CatalogInfoKey('layers@' + #resource.id, 'LAYER')",
            unless = "#result.isEmpty()")
    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return super.getLayers(resource);
    }

    @Cacheable(key = "'" + DEFAULT_WORKSPACE_CACHE_KEY + "'", unless = "#result == null")
    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return super.getDefaultWorkspace();
    }

    @CacheEvict(key = "'" + DEFAULT_WORKSPACE_CACHE_KEY + "'")
    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        super.setDefaultWorkspace(workspace);
    }

    @Cacheable(key = "'" + DEFAULT_NAMESPACE_CACHE_KEY + "'", unless = "#result == null")
    @Override
    public NamespaceInfo getDefaultNamespace() {
        return super.getDefaultNamespace();
    }

    @CacheEvict(key = "'" + DEFAULT_NAMESPACE_CACHE_KEY + "'")
    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        super.setDefaultNamespace(defaultNamespace);
    }

    @Cacheable(
            key = "'" + DEFAULT_DATASTORE_CACHE_KEY_PREFIX + "' + #p0.id",
            unless = "#result == null")
    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return super.getDefaultDataStore(workspace);
    }

    @CacheEvict(key = "'" + DEFAULT_DATASTORE_CACHE_KEY_PREFIX + "' + #p0.id")
    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        super.setDefaultDataStore(workspace, store);
    }
}
