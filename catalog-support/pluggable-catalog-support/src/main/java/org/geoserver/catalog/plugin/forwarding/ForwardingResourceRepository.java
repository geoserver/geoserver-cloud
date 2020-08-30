/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.List;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;

public class ForwardingResourceRepository extends ForwardingCatalogRepository<ResourceInfo>
        implements ResourceRepository {

    public ForwardingResourceRepository(ResourceRepository subject) {
        super(subject);
    }

    public @Override <T extends ResourceInfo> T findOneByName(String name, Class<T> clazz) {
        return ((ResourceRepository) subject).findOneByName(name, clazz);
    }

    public @Override <T extends ResourceInfo> List<T> findAllByType(Class<T> clazz) {
        return ((ResourceRepository) subject).findAllByType(clazz);
    }

    public @Override <T extends ResourceInfo> List<T> findAllByNamespace(
            NamespaceInfo ns, Class<T> clazz) {
        return ((ResourceRepository) subject).findAllByNamespace(ns, clazz);
    }

    public @Override <T extends ResourceInfo> T findByStoreAndName(
            StoreInfo store, String name, Class<T> clazz) {
        return ((ResourceRepository) subject).findByStoreAndName(store, name, clazz);
    }

    public @Override <T extends ResourceInfo> List<T> findAllByStore(
            StoreInfo store, Class<T> clazz) {
        return ((ResourceRepository) subject).findAllByStore(store, clazz);
    }
}
