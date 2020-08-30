/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.repository.caching;

import java.util.List;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;

@CacheConfig(cacheNames = CacheNames.STYLE_CACHE)
public class CachingStyleRepository extends CachingCatalogRepository<StyleInfo>
        implements StyleRepository {

    public CachingStyleRepository(StyleRepository subject) {
        super(subject);
    }

    @Cacheable
    public @Override StyleInfo findOneByName(String name) {
        return ((StyleRepository) subject).findOneByName(name);
    }

    public @Override List<StyleInfo> findAllByNullWorkspace() {
        return ((StyleRepository) subject).findAllByNullWorkspace();
    }

    public @Override List<StyleInfo> findAllByWorkspace(WorkspaceInfo ws) {
        return ((StyleRepository) subject).findAllByWorkspace(ws);
    }
}
