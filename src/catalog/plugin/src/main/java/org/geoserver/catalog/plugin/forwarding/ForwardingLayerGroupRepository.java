/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;

public class ForwardingLayerGroupRepository extends ForwardingCatalogRepository<LayerGroupInfo, LayerGroupRepository>
        implements LayerGroupRepository {

    public ForwardingLayerGroupRepository(LayerGroupRepository subject) {
        super(subject);
    }

    @Override
    public Stream<LayerGroupInfo> findAllByWorkspaceIsNull() {
        return subject.findAllByWorkspaceIsNull();
    }

    @Override
    public Stream<LayerGroupInfo> findAllByWorkspace(WorkspaceInfo workspace) {
        return subject.findAllByWorkspace(workspace);
    }

    @Override
    public Optional<LayerGroupInfo> findByNameAndWorkspaceIsNull(@NonNull String name) {
        return subject.findByNameAndWorkspaceIsNull(name);
    }

    @Override
    public Optional<LayerGroupInfo> findByNameAndWorkspace(String name, WorkspaceInfo workspace) {
        return subject.findByNameAndWorkspace(name, workspace);
    }
}
