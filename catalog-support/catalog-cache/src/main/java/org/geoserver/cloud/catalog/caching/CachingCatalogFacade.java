/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

import lombok.NonNull;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.springframework.cache.CacheManager;

/** */
public interface CachingCatalogFacade extends ExtendedCatalogFacade {

    /**
     * Name of the cache used by {@link CachingCatalogFacadeImpl}, used as key to acquire it through
     * {@link CacheManager#getCache(String)}
     */
    String CACHE_NAME = "gs-catalog";

    /** Key used to cache and evict {@link CatalogFacade#getDefaultNamespace() default namespace} */
    String DEFAULT_NAMESPACE_CACHE_KEY = "defaultNamespace";

    /** Key used to cache and evict {@link CatalogFacade#getDefaultWorkspace() default workspace} */
    String DEFAULT_WORKSPACE_CACHE_KEY = "defaultWorkspace";

    /**
     * Prefix used to build a per-workspace-id default datastore cache key
     *
     * @see #generateDefaultDataStoreKey
     */
    String DEFAULT_DATASTORE_CACHE_KEY_PREFIX = "defaultDataStore.";

    /**
     * Evicts the given object from the cache
     *
     * @param info a {@link CatalogInfo} object to evict, can be a proxy as long as it provides a
     *     proper {@link Info#getId() id}
     */
    <C extends CatalogInfo> boolean evict(C info);

    boolean evict(@NonNull Object key);

    static Object generateDefaultDataStoreKey(WorkspaceInfo workspace) {
        return DEFAULT_DATASTORE_CACHE_KEY_PREFIX + workspace.getId();
    }

    static CatalogInfoKey generateLayersByResourceKey(ResourceInfo resource) {
        return new CatalogInfoKey("layers@" + resource.getId(), ClassMappings.LAYER);
    }
}
