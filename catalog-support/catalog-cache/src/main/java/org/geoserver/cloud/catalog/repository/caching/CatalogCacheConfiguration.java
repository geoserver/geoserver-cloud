/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.repository.caching;

import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

/** */
@EnableCaching
public class CatalogCacheConfiguration {

    @ConditionalOnBean(value = WorkspaceRepository.class)
    public @Bean WorkspaceRepository workspaceRepository(
            @Qualifier("WorkspaceRepository") WorkspaceRepository repo) {
        return new CachingWorkspaceRepository(repo);
    }
}
