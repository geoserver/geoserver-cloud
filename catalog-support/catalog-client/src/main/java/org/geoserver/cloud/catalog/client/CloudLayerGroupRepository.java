/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client;

import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.cloud.catalog.client.feign.LayerGroupClient;

public class CloudLayerGroupRepository
        extends CatalogServiceClientRepository<LayerGroupInfo, LayerGroupClient>
        implements LayerGroupRepository {

    private final @Getter Class<LayerGroupInfo> infoType = LayerGroupInfo.class;

    protected CloudLayerGroupRepository(@NonNull LayerGroupClient client) {
        super(client);
    }

    public @Override LayerGroupInfo findOneByName(String name) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override List<LayerGroupInfo> findAllByWorkspaceIsNull() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override List<LayerGroupInfo> findAllByWorkspace(WorkspaceInfo workspace) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
