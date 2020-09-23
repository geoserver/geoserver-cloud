/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.repository.caching;

import java.util.Optional;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.plugin.forwarding.ForwardingLayerRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;

@CacheConfig(cacheNames = CacheNames.LAYER_CACHE)
public class CachingLayerRepository extends ForwardingLayerRepository {

    public CachingLayerRepository(LayerRepository subject) {
        super(subject);
    }

    @Cacheable
    public @Override Optional<LayerInfo> findOneByName(String name) {
        return super.findOneByName(name);
    }
}
