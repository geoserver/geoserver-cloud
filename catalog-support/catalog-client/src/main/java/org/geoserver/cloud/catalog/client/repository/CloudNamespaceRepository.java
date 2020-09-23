/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.springframework.lang.Nullable;

public class CloudNamespaceRepository extends CatalogServiceClientRepository<NamespaceInfo>
        implements NamespaceRepository {

    private final @Getter Class<NamespaceInfo> infoType = NamespaceInfo.class;

    public @Override void setDefaultNamespace(@NonNull NamespaceInfo namespace) {
        Objects.requireNonNull(namespace.getId(), "provided null namespace id");
        callAndBlock(() -> client().setDefaultNamespace(namespace.getId()));
    }

    public @Override @Nullable NamespaceInfo getDefaultNamespace() {
        return callAndReturn(client()::getDefaultNamespace);
    }

    public @Override @Nullable NamespaceInfo findOneByURI(@NonNull String uri) {
        return callAndReturn(() -> client().findOneNamespaceByURI(uri));
    }

    public @Override Stream<NamespaceInfo> findAllByURI(@NonNull String uri) {
        return client().findAllNamespacesByURI(uri).map(this::resolve).toStream();
    }
}
