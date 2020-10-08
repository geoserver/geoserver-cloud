/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

import java.util.Optional;
import lombok.NonNull;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.plugin.forwarding.ForwardingResourceRepository;
import org.springframework.cache.annotation.CacheConfig;

@CacheConfig(cacheNames = CacheNames.RESOURCE_CACHE)
public class CachingResourceRepository extends ForwardingResourceRepository {

    public CachingResourceRepository(ResourceRepository subject) {
        super(subject);
    }

    public @Override <T extends ResourceInfo> Optional<T> findByStoreAndName(
            StoreInfo store, String name, Class<T> clazz) {
        return super.findByStoreAndName(store, name, clazz);
    }

    public @Override <T extends ResourceInfo> Optional<T> findByNameAndNamespace(
            @NonNull String name, @NonNull NamespaceInfo namespace, Class<T> clazz) {
        return super.findByNameAndNamespace(name, namespace, clazz);
    }
}
