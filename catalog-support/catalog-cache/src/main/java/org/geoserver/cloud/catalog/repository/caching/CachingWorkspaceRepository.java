/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.repository.caching;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

@CacheConfig(cacheNames = CacheNames.WORKSPACE_CACHE)
public class CachingWorkspaceRepository extends CachingCatalogRepository<WorkspaceInfo>
        implements WorkspaceRepository {

    public CachingWorkspaceRepository(WorkspaceRepository subject) {
        super(subject);
    }

    @CachePut(key = "defaultWorkspace")
    public @Override void setDefaultWorkspace(WorkspaceInfo workspace) {
        ((WorkspaceRepository) subject).setDefaultWorkspace(workspace);
    }

    @Cacheable(key = "defaultWorkspace")
    public @Override WorkspaceInfo getDefaultWorkspace() {
        return ((WorkspaceRepository) subject).getDefaultWorkspace();
    }
}
