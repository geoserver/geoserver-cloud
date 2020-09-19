/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.Objects;
import java.util.stream.Stream;
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

    public @Override Stream<LayerGroupInfo> findAllByWorkspaceIsNull() {
        return client().findLayerGroupsByNullWoskspace().toStream();
    }

    public @Override Stream<LayerGroupInfo> findAllByWorkspace(@NonNull WorkspaceInfo workspace) {
        return client().findLayerGroupsByWoskspaceId(workspace.getId()).toStream();
    }

    @Override
    public LayerGroupInfo findByNameAndWorkspaceIsNull(@NonNull String name) {
        return client().findLayerGropuByNameAndNullWorkspace(name).block();
    }

    @Override
    public LayerGroupInfo findByNameAndWorkspace(@NonNull String name,
            @NonNull WorkspaceInfo workspace) {
        Objects.requireNonNull(workspace.getId());
        return client().findLayerGropuByNameAndWorkspaceId(name, workspace.getId()).block();
    }
}
