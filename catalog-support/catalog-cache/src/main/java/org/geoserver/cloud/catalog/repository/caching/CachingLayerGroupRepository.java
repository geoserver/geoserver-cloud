/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.repository.caching;

import lombok.NonNull;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.forwarding.ForwardingLayerGroupRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;

@CacheConfig(cacheNames = CacheNames.LAYER_GROUP_CACHE)
public class CachingLayerGroupRepository extends ForwardingLayerGroupRepository {

    public CachingLayerGroupRepository(LayerGroupRepository subject) {
        super(subject);
    }

    @Cacheable
    public @Override LayerGroupInfo findByNameAndWorkspaceIsNull(@NonNull String name) {
        return super.findByNameAndWorkspaceIsNull(name);
    }

    @Cacheable
    public @Override LayerGroupInfo findByNameAndWorkspace(String name, WorkspaceInfo workspace) {
        return super.findByNameAndWorkspace(name, workspace);
    }
}
