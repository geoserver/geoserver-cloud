/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import lombok.Getter;
import lombok.NonNull;

import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class CatalogClientNamespaceRepository extends CatalogClientRepository<NamespaceInfo>
        implements NamespaceRepository {

    private final @Getter Class<NamespaceInfo> contentType = NamespaceInfo.class;

    @Override public  void setDefaultNamespace(@NonNull NamespaceInfo namespace) {
        Objects.requireNonNull(namespace.getId(), "provided null namespace id");
        blockAndReturn(client().setDefaultNamespace(namespace.getId()));
    }

    @Override public  <U extends NamespaceInfo> Optional<U> findFirstByName(
            @NonNull String name, @NonNull Class<U> infoType) {
        // geoserver has this tendency to loose method contracts...
        if (name.indexOf(':') > -1) {
            return Optional.empty();
        }
        return super.findFirstByName(name, infoType);
    }

    @Override public  void unsetDefaultNamespace() {
        block(client().unsetDefaultNamespace());
    }

    @Override public  @Nullable Optional<NamespaceInfo> getDefaultNamespace() {
        return blockAndReturn(client().getDefaultNamespace());
    }

    @Override public  @Nullable Optional<NamespaceInfo> findOneByURI(@NonNull String uri) {
        return blockAndReturn(client().findOneNamespaceByURI(uri));
    }

    @Override public  Stream<NamespaceInfo> findAllByURI(@NonNull String uri) {
        return toStream(client().findAllNamespacesByURI(uri));
    }
}
