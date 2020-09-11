/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.cloud.catalog.client.feign.NamespaceClient;
import org.springframework.lang.Nullable;

public class CloudNamespaceRepository
        extends CatalogServiceClientRepository<NamespaceInfo, NamespaceClient>
        implements NamespaceRepository {

    private final @Getter Class<NamespaceInfo> infoType = NamespaceInfo.class;

    protected CloudNamespaceRepository(@NonNull NamespaceClient client) {
        super(client);
    }

    public @Override void setDefaultNamespace(@NonNull NamespaceInfo namespace) {
        client().setDefault(namespace);
    }

    public @Override @Nullable NamespaceInfo getDefaultNamespace() {
        return client().getDefault();
    }

    public @Override @Nullable NamespaceInfo findOneByURI(@NonNull String uri) {
        return client().findFirstByURI(uri);
    }

    public @Override List<NamespaceInfo> findAllByURI(@NonNull String uri) {
        return client().findAllByURI(uri);
    }
}
