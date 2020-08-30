/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.repository.caching;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.forwarding.ForwardingCatalogRepository;
import org.opengis.feature.type.Name;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

@CacheConfig(keyGenerator = CacheNames.DEFAULT_KEY_GENERATOR_BEAN_NAME)
public abstract class CachingCatalogRepository<I extends CatalogInfo>
        extends ForwardingCatalogRepository<I> {

    public CachingCatalogRepository(CatalogInfoRepository<I> subject) {
        super(subject);
    }

    @CachePut
    public @Override void add(I value) {
        super.add(value);
    }

    @CacheEvict
    public @Override void remove(I value) {
        super.remove(value);
    }

    @CacheEvict
    public @Override void update(I value) {
        super.update(value);
    }

    @Cacheable
    public @Override <U extends I> U findById(String id, Class<U> clazz) {
        return super.findById(id, clazz);
    }

    @Cacheable
    public @Override <U extends I> U findByName(Name name, Class<U> clazz) {
        return super.findByName(name, clazz);
    }
}
