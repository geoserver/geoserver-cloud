/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client;

import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.cloud.catalog.client.feign.StyleClient;

public class CloudStyleRepository extends CatalogServiceClientRepository<StyleInfo, StyleClient>
        implements StyleRepository {

    private final @Getter Class<StyleInfo> infoType = StyleInfo.class;

    protected CloudStyleRepository(@NonNull StyleClient client) {
        super(client);
    }

    public @Override StyleInfo findOneByName(String name) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override List<StyleInfo> findAllByNullWorkspace() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override List<StyleInfo> findAllByWorkspace(WorkspaceInfo ws) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
