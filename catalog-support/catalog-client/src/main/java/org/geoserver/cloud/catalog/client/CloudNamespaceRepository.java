/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client;

import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.cloud.catalog.client.feign.NamespaceClient;

public class CloudNamespaceRepository
        extends CatalogServiceClientRepository<NamespaceInfo, NamespaceClient>
        implements NamespaceRepository {

    private final @Getter Class<NamespaceInfo> infoType = NamespaceInfo.class;

    protected CloudNamespaceRepository(@NonNull NamespaceClient client) {
        super(client);
    }

    public @Override void setDefaultNamespace(NamespaceInfo namespace) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override NamespaceInfo getDefaultNamespace() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override NamespaceInfo findOneByURI(String uri) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override List<NamespaceInfo> findAllByURI(String uri) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
