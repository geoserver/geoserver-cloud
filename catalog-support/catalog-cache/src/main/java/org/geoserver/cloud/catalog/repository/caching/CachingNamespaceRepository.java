/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.repository.caching;

import java.util.List;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

@CacheConfig(cacheNames = CacheNames.NAMESPACE_CACHE)
public class CachingNamespaceRepository extends CachingCatalogRepository<NamespaceInfo>
        implements NamespaceRepository {

    public CachingNamespaceRepository(NamespaceRepository subject) {
        super(subject);
    }

    @CachePut(key = "defaultNamespace")
    public @Override void setDefaultNamespace(NamespaceInfo namespace) {
        ((NamespaceRepository) subject).setDefaultNamespace(namespace);
    }

    @Cacheable(key = "defaultNamespace")
    public @Override NamespaceInfo getDefaultNamespace() {
        return ((NamespaceRepository) subject).getDefaultNamespace();
    }

    @Cacheable
    public @Override NamespaceInfo findOneByURI(String uri) {
        return ((NamespaceRepository) subject).findOneByURI(uri);
    }

    public @Override List<NamespaceInfo> findAllByURI(String uri) {
        return ((NamespaceRepository) subject).findAllByURI(uri);
    }
}
