/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.catalog.cache;

import static org.geoserver.cloud.event.info.ConfigInfoType.RESOURCE;
import static org.geoserver.cloud.event.info.ConfigInfoType.STORE;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

/**
 * @since 1.7
 */
@ParametersAreNonnullByDefault
@Slf4j(topic = "org.geoserver.cloud.catalog.cache")
class CachingCatalogFacadeContainmentSupport {
    /** Key used to cache and evict {@link CatalogFacade#getDefaultNamespace() default namespace} */
    static final String DEFAULT_NAMESPACE_CACHE_KEY = "defaultNamespace";

    /** Key used to cache and evict {@link CatalogFacade#getDefaultWorkspace() default workspace} */
    static final String DEFAULT_WORKSPACE_CACHE_KEY = "defaultWorkspace";

    /**
     * Prefix used to build a per-workspace-id default datastore cache key
     *
     * @see #generateDefaultDataStoreKey
     */
    static final String DEFAULT_DATASTORE_CACHE_KEY_PREFIX = "defaultDataStore@";

    @Getter(value = AccessLevel.PACKAGE)
    private final @NonNull Cache cache;

    /**
     * Cascade evicts cached {@link CatalogInfo} objects that directly or indirectly reference an
     * object called to be evicted
     */
    private final CachedReferenceCleaner referenceCleaner;

    public CachingCatalogFacadeContainmentSupport(@NonNull Cache cache) {
        this.cache = cache;
        this.referenceCleaner = CachedReferenceCleaner.newInstance(cache);
    }

    @VisibleForTesting
    CachingCatalogFacadeContainmentSupport() {
        this(newCache());
    }

    private static @NonNull Cache newCache() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CachingCatalogFacade.CACHE_NAME);
        return Objects.requireNonNull(manager.getCache(CachingCatalogFacade.CACHE_NAME));
    }

    public void evictAll() {
        cache.clear();
    }

    /**
     * Evicts the {@link InfoIdKey} entry used for id lookups and the {@link InfoNameKey} used for
     * name lookups.
     *
     * <p>For {@link ResourceInfo} concrete-types, also evicts the entries for {@link
     * ConfigInfoType#RESOURCE}, accounting for {@link #getResource(String, Class) getResource(id,
     * ResourceInfo.class)} cached entries.
     *
     * <p>For {@link StoreInfo} concrete-types, also evicts the entries for {@link
     * ConfigInfoType#STORE}, accounting for {@link #getStore(String, Class) getStore(id,
     * StoreInfo.class)} cached entries.
     *
     * <p>For {@link ResourceInfo} and {@link PublishedInfo} sub-types, also evicts any {@link
     * LayerGroupInfo} key directly or indirectly referencing it.
     *
     * <p>For {@link StyleInfo}, also evict the entries for any {@link LayerInfo} and {@link
     * LayerGroupInfo} referencing the style.
     */
    public boolean evict(String id, String prefixedName, ConfigInfoType type) {
        return evictInternal(id, prefixedName, type, true);
    }

    private boolean evictInternal(String id, String prefixedName, ConfigInfoType type, boolean doLog) {
        boolean evicted = false;
        if (type.isA(WorkspaceInfo.class)) {
            evictDefaultDataStore(id, prefixedName);
        } else if (type.isA(StoreInfo.class) && type != STORE) {
            evicted |= evict(id, prefixedName, STORE);
        } else if (type.isA(ResourceInfo.class) && type != RESOURCE) {
            evicted |= evict(id, prefixedName, RESOURCE);
        }

        InfoIdKey idKey = new InfoIdKey(id, type);
        InfoNameKey nameKey = new InfoNameKey(prefixedName, type);

        evicted |= evict(idKey);
        evicted |= evict(nameKey);
        if (evicted && doLog) {
            log.debug("evicted {}[id: {}, name: {}]", type.type().getSimpleName(), id, prefixedName);
        }
        // regardless of the object being evicted or not, cascade evict any entry referencing it
        referenceCleaner.cascadeEvict(idKey);
        return evicted;
    }

    boolean evict(Object key) {
        boolean evicted = cache.evictIfPresent(key);
        if (evicted) {
            log.trace("evicted {}", key);
        }
        return evicted;
    }

    public boolean evict(CatalogInfo info) {
        if (info instanceof LayerInfo l) {
            evict(generateLayersByResourceKey(l.getResource()));
        }
        return evict(info.getId(), InfoEvent.prefixedName(info), InfoEvent.typeOf(info));
    }

    public <T extends CatalogInfo> T evictAndGet(T info, Callable<T> loader) {
        evict(info);
        return put(loader);
    }

    @SneakyThrows
    <T extends CatalogInfo> T put(Callable<T> supplier) {
        T info = supplier.call();
        if (null != info) {
            put(InfoIdKey.valueOf(info), info);
            put(InfoNameKey.valueOf(info), info);
        }
        return info;
    }

    private void put(Object key, Object value) {
        cache.put(key, value);
    }

    public <T> T get(Object key, Callable<T> loader) {
        T value = cache.get(key, loader);
        if (null == value || (value instanceof Collection<?> c && c.isEmpty())) {
            cache.evict(key);
        }
        return value;
    }

    public List<LayerInfo> getLayersByResource(String resourceInfoId, Callable<List<LayerInfo>> loader) {
        InfoIdKey key = generateLayersByResourceKey(resourceInfoId);
        return get(key, loader);
    }

    Object generateDefaultDataStoreKey(String workspaceId) {
        return DEFAULT_DATASTORE_CACHE_KEY_PREFIX + workspaceId;
    }

    InfoIdKey generateLayersByResourceKey(ResourceInfo resource) {
        return generateLayersByResourceKey(resource.getId());
    }

    InfoIdKey generateLayersByResourceKey(String resourceId) {
        return new InfoIdKey("layers@" + resourceId, ConfigInfoType.LAYER);
    }

    public WorkspaceInfo getDefaultWorkspace(Callable<WorkspaceInfo> loader) {
        return cache.get(DEFAULT_WORKSPACE_CACHE_KEY, loader);
    }

    public void evictDefaultWorkspace() {
        if (evict(DEFAULT_WORKSPACE_CACHE_KEY)) {
            log.trace("default workspace evicted");
        }
    }

    public NamespaceInfo getDefaultNamespace(Callable<NamespaceInfo> loader) {
        return cache.get(DEFAULT_NAMESPACE_CACHE_KEY, loader);
    }

    public void evictDefaultNamespace() {
        if (evict(DEFAULT_NAMESPACE_CACHE_KEY)) {
            log.trace("default namespace evicted");
        }
    }

    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace, Callable<DataStoreInfo> loader) {
        return cache.get(generateDefaultDataStoreKey(workspace.getId()), loader);
    }

    public void evictDefaultDataStore(WorkspaceInfo ws) {
        evictDefaultDataStore(ws.getId(), ws.getName());
    }

    public void evictDefaultDataStore(String workspaceId, String name) {
        Object key = generateDefaultDataStoreKey(workspaceId);
        if (cache.evictIfPresent(key)) {
            log.trace("evicted default datastore for workspace {}", name);
        }
    }
}
