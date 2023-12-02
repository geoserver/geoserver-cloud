/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import lombok.Getter;
import lombok.NonNull;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class CatalogClientLayerGroupRepository extends CatalogClientRepository<LayerGroupInfo>
        implements LayerGroupRepository {

    private final @Getter Class<LayerGroupInfo> contentType = LayerGroupInfo.class;

    @Override public  Stream<LayerGroupInfo> findAllByWorkspaceIsNull() {
        return toStream(client().findLayerGroupsByNullWoskspace());
    }

    @Override public  Stream<LayerGroupInfo> findAllByWorkspace(@NonNull WorkspaceInfo workspace) {
        return toStream(client().findLayerGroupsByWoskspaceId(workspace.getId()));
    }

    @Override public  Optional<LayerGroupInfo> findByNameAndWorkspaceIsNull(@NonNull String name) {
        return blockAndReturn(client().findLayerGroupByNameAndNullWorkspace(name));
    }

    @Override public  Optional<LayerGroupInfo> findByNameAndWorkspace(
            @NonNull String name, @NonNull WorkspaceInfo workspace) {
        Objects.requireNonNull(workspace.getId());
        return blockAndReturn(client().findLayerGroupByWorkspaceIdAndName(workspace.getId(), name));
    }
}
