/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;

public class CloudStyleRepository extends CatalogServiceClientRepository<StyleInfo>
        implements StyleRepository {

    private final @Getter Class<StyleInfo> infoType = StyleInfo.class;

    public @Override Stream<StyleInfo> findAllByNullWorkspace() {
        return client().findStylesByNullWorkspace().toStream();
    }

    public @Override Stream<StyleInfo> findAllByWorkspace(@NonNull WorkspaceInfo ws) {
        return client().findStylesByWorkspaceId(ws.getId()).toStream();
    }

    public @Override StyleInfo findByNameAndWordkspaceNull(@NonNull String name) {
        return client().findStyleByNameAndNullWorkspace(name).block();
    }

    public @Override StyleInfo findByNameAndWordkspace(
            @NonNull String name, @NonNull WorkspaceInfo workspace) {
        return client().findStyleByWorkspaceIdAndName(workspace.getId(), name).block();
    }
}
