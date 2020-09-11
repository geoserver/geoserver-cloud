/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.repository.caching;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.forwarding.ForwardingStoreRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

@CacheConfig(cacheNames = CacheNames.STORE_CACHE)
public class CachingStoreRepository extends ForwardingStoreRepository {

    public CachingStoreRepository(StoreRepository subject) {
        super(subject);
    }

    @CachePut
    public @Override void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo dataStore) {
        super.setDefaultDataStore(workspace, dataStore);
    }

    @Cacheable
    public @Override DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return super.getDefaultDataStore(workspace);
    }

    @Cacheable
    public @Override <T extends StoreInfo> T findByNameAndWorkspace(
            String name, WorkspaceInfo workspace, Class<T> clazz) {
        return super.findByNameAndWorkspace(name, workspace, clazz);
    }
}
