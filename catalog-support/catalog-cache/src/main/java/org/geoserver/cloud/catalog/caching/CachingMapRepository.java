/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

import org.geoserver.catalog.plugin.forwarding.ForwardingMapRepository;
import org.springframework.cache.annotation.CacheConfig;

@CacheConfig(cacheNames = CacheNames.MAP_CACHE)
public class CachingMapRepository extends ForwardingMapRepository {

    public CachingMapRepository(MapRepository subject) {
        super(subject);
    }
}
