/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.repository.caching;

import java.util.List;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;

@CacheConfig(cacheNames = CacheNames.LAYER_GROUP_CACHE)
public class CachingLayerGroupRepository extends CachingCatalogRepository<LayerGroupInfo>
        implements LayerGroupRepository {

    public CachingLayerGroupRepository(LayerGroupRepository subject) {
        super(subject);
    }

    @Cacheable
    public @Override LayerGroupInfo findOneByName(String name) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override List<LayerGroupInfo> findAllByWorkspaceIsNull() {
        return ((LayerGroupRepository) subject).findAllByWorkspaceIsNull();
    }

    public @Override List<LayerGroupInfo> findAllByWorkspace(WorkspaceInfo workspace) {
        return ((LayerGroupRepository) subject).findAllByWorkspace(workspace);
    }
}
