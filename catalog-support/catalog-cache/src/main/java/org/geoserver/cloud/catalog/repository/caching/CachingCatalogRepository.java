/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.repository.caching;

import java.util.Optional;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.forwarding.ForwardingCatalogRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

// Using simple cache by id right now, no need for a specialized key generator
// @CacheConfig(keyGenerator = CacheNames.DEFAULT_KEY_GENERATOR_BEAN_NAME)
public abstract class CachingCatalogRepository<
                I extends CatalogInfo, S extends CatalogInfoRepository<I>>
        extends ForwardingCatalogRepository<I, S> {

    public CachingCatalogRepository(S subject) {
        super(subject);
    }

    @CachePut(key = "#p0.id")
    public @Override void add(I value) {
        super.add(value);
    }

    @CacheEvict(key = "#p0.id")
    public @Override void remove(I value) {
        super.remove(value);
    }

    @CacheEvict(key = "#p0.id")
    public @Override <T extends I> T update(T value, Patch patch) {
        return super.update(value, patch);
    }

    @Cacheable(key = "#p0")
    public @Override <U extends I> Optional<U> findById(String id, Class<U> clazz) {
        return super.findById(id, clazz);
    }

    public @Override <U extends I> Optional<U> findFirstByName(String name, Class<U> clazz) {
        return super.findFirstByName(name, clazz);
    }
}
