/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

public class CloudNamespaceRepository extends CatalogServiceClientRepository<NamespaceInfo>
        implements NamespaceRepository {

    private final @Getter Class<NamespaceInfo> contentType = NamespaceInfo.class;

    public @Override void setDefaultNamespace(@NonNull NamespaceInfo namespace) {
        Objects.requireNonNull(namespace.getId(), "provided null namespace id");
        blockAndReturn(client().setDefaultNamespace(namespace.getId()));
    }

    public @Override void unsetDefaultNamespace() {
        Mono<Void> call = client().unsetDefaultNamespace();
        call.block();
    }

    public @Override @Nullable Optional<NamespaceInfo> getDefaultNamespace() {
        return blockAndReturn(client().getDefaultNamespace());
    }

    public @Override @Nullable Optional<NamespaceInfo> findOneByURI(@NonNull String uri) {
        return blockAndReturn(client().findOneNamespaceByURI(uri));
    }

    public @Override Stream<NamespaceInfo> findAllByURI(@NonNull String uri) {
        return client().findAllNamespacesByURI(uri).map(this::resolve).toStream();
    }
}
