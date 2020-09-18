/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.List;
import java.util.Objects;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient;
import org.springframework.lang.Nullable;
import lombok.Getter;
import lombok.NonNull;

public class CloudNamespaceRepository extends CatalogServiceClientRepository<NamespaceInfo>
        implements NamespaceRepository {

    private final @Getter Class<NamespaceInfo> infoType = NamespaceInfo.class;

    protected CloudNamespaceRepository(@NonNull ReactiveCatalogClient client) {
        super(client);
    }

    public @Override void setDefaultNamespace(@NonNull NamespaceInfo namespace) {
        Objects.requireNonNull(namespace.getId(), "provided null namespace id");
        client().setDefaultNamespace(namespace.getId());
    }

    public @Override @Nullable NamespaceInfo getDefaultNamespace() {
        return client().getDefaultNamespace().block();
    }

    public @Override @Nullable NamespaceInfo findOneByURI(@NonNull String uri) {
        return client().findOneNamespaceByURI(uri).block();
    }

    public @Override List<NamespaceInfo> findAllByURI(@NonNull String uri) {
        return client().findAllNamespacesByURI(uri).collectList().block();
    }
}
