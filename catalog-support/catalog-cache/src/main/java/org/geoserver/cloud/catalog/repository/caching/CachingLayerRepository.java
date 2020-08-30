/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.repository.caching;

import java.util.List;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;

@CacheConfig(cacheNames = CacheNames.LAYER_CACHE)
public class CachingLayerRepository extends CachingCatalogRepository<LayerInfo>
        implements LayerRepository {

    public CachingLayerRepository(LayerRepository subject) {
        super(subject);
    }

    @Cacheable
    public @Override LayerInfo findOneByName(String name) {
        return ((LayerRepository) subject).findOneByName(name);
    }

    public @Override List<LayerInfo> findAllByDefaultStyleOrStyles(StyleInfo style) {
        return ((LayerRepository) subject).findAllByDefaultStyleOrStyles(style);
    }

    public @Override List<LayerInfo> findAllByResource(ResourceInfo resource) {
        return ((LayerRepository) subject).findAllByResource(resource);
    }
}
