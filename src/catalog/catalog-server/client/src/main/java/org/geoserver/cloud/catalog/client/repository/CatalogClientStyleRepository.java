/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import lombok.Getter;
import lombok.NonNull;

import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;

import java.util.Optional;
import java.util.stream.Stream;

public class CatalogClientStyleRepository extends CatalogClientRepository<StyleInfo>
        implements StyleRepository {

    private final @Getter Class<StyleInfo> contentType = StyleInfo.class;

    public @Override Stream<StyleInfo> findAllByNullWorkspace() {
        return toStream(client().findStylesByNullWorkspace());
    }

    public @Override Stream<StyleInfo> findAllByWorkspace(@NonNull WorkspaceInfo ws) {
        return toStream(client().findStylesByWorkspaceId(ws.getId()));
    }

    public @Override Optional<StyleInfo> findByNameAndWordkspaceNull(@NonNull String name) {
        return blockAndReturn(client().findStyleByNameAndNullWorkspace(name));
    }

    public @Override Optional<StyleInfo> findByNameAndWorkspace(
            @NonNull String name, @NonNull WorkspaceInfo workspace) {

        String workspaceId = workspace.getId();
        return blockAndReturn(client().findStyleByWorkspaceIdAndName(workspaceId, name));
    }
}
