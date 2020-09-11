/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.List;
import lombok.NonNull;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;

public class ForwardingLayerGroupRepository
        extends ForwardingCatalogRepository<LayerGroupInfo, LayerGroupRepository>
        implements LayerGroupRepository {

    public ForwardingLayerGroupRepository(LayerGroupRepository subject) {
        super(subject);
    }

    public @Override List<LayerGroupInfo> findAllByWorkspaceIsNull() {
        return subject.findAllByWorkspaceIsNull();
    }

    public @Override List<LayerGroupInfo> findAllByWorkspace(WorkspaceInfo workspace) {
        return subject.findAllByWorkspace(workspace);
    }

    public @Override LayerGroupInfo findByNameAndWorkspaceIsNull(@NonNull String name) {
        return subject.findByNameAndWorkspaceIsNull(name);
    }

    public @Override LayerGroupInfo findByNameAndWorkspace(String name, WorkspaceInfo workspace) {
        return subject.findByNameAndWorkspace(name, workspace);
    }
}
