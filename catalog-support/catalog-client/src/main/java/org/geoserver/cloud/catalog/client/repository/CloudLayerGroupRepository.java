/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

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

    public @Override List<LayerGroupInfo> findAllByWorkspaceIsNull() {
        return client().findAllByWoskspaceIsNull();
    }

    public @Override List<LayerGroupInfo> findAllByWorkspace(@NonNull WorkspaceInfo workspace) {
        return client().findAllByWoskspaceId(workspace.getId());
    }

    @Override
    public LayerGroupInfo findByNameAndWorkspaceIsNull(@NonNull String name) {
        return client().findByNameAndWorkspaceId(name, null);
    }

    @Override
    public LayerGroupInfo findByNameAndWorkspace(String name, WorkspaceInfo workspace) {
        return client().findByNameAndWorkspaceId(name, workspace.getId());
    }
}
