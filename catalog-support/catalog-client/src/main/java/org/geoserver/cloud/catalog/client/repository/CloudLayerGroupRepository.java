/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.List;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient;
import lombok.Getter;
import lombok.NonNull;

public class CloudLayerGroupRepository extends CatalogServiceClientRepository<LayerGroupInfo>
        implements LayerGroupRepository {

    private final @Getter Class<LayerGroupInfo> infoType = LayerGroupInfo.class;

    protected CloudLayerGroupRepository(@NonNull ReactiveCatalogClient client) {
        super(client);
    }

    public @Override List<LayerGroupInfo> findAllByWorkspaceIsNull() {
        return client().findLayerGroupsByNullWoskspace();
    }

    public @Override List<LayerGroupInfo> findAllByWorkspace(@NonNull WorkspaceInfo workspace) {
        return client().findLayerGroupsByWoskspaceId(workspace.getId());
    }

    @Override
    public LayerGroupInfo findByNameAndWorkspaceIsNull(@NonNull String name) {}

    @Override
    public LayerGroupInfo findByNameAndWorkspace(String name, WorkspaceInfo workspace) {}
}
