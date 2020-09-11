/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.cloud.catalog.client.feign.ResourceClient;
import org.springframework.lang.Nullable;

public class CloudResourceRepository
        extends CatalogServiceClientRepository<ResourceInfo, ResourceClient>
        implements ResourceRepository {

    private final @Getter Class<ResourceInfo> infoType = ResourceInfo.class;

    protected CloudResourceRepository(@NonNull ResourceClient client) {
        super(client);
    }

    public @Override <T extends ResourceInfo> List<T> findAllByType(@Nullable Class<T> clazz) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override <T extends ResourceInfo> List<T> findAllByNamespace(
            @NonNull NamespaceInfo ns, @Nullable Class<T> clazz) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override @Nullable <T extends ResourceInfo> T findByStoreAndName(
            @NonNull StoreInfo store, @NonNull String name, @Nullable Class<T> clazz) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override <T extends ResourceInfo> List<T> findAllByStore(
            StoreInfo store, Class<T> clazz) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override <T extends ResourceInfo> T findByNameAndNamespace(
            @NonNull String name, @NonNull NamespaceInfo namespace, @NonNull Class<T> clazz) {
        return clazz.cast(
                client().findByNameAndNamespaceId(name, namespace.getId(), typeEnum(clazz)));
    }
}
