/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.repository.caching;

import java.util.Optional;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.plugin.forwarding.ForwardingNamespaceRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

@CacheConfig(cacheNames = CacheNames.NAMESPACE_CACHE)
public class CachingNamespaceRepository extends ForwardingNamespaceRepository {

    public CachingNamespaceRepository(NamespaceRepository subject) {
        super(subject);
    }

    @CachePut(key = "defaultNamespace")
    public @Override void setDefaultNamespace(NamespaceInfo namespace) {
        super.setDefaultNamespace(namespace);
    }

    @CacheEvict(key = "defaultNamespace")
    public @Override void unsetDefaultNamespace() {
        super.unsetDefaultNamespace();
    }

    @Cacheable(key = "defaultNamespace")
    public @Override Optional<NamespaceInfo> getDefaultNamespace() {
        return super.getDefaultNamespace();
    }

    @Cacheable
    public @Override Optional<NamespaceInfo> findOneByURI(String uri) {
        return super.findOneByURI(uri);
    }
}
