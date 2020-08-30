/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.List;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;

public class ForwardingLayerGroupRepository extends ForwardingCatalogRepository<LayerGroupInfo>
        implements LayerGroupRepository {

    public ForwardingLayerGroupRepository(LayerGroupRepository subject) {
        super(subject);
    }

    public @Override LayerGroupInfo findOneByName(String name) {
        return ((LayerGroupRepository) subject).findOneByName(name);
    }

    public @Override List<LayerGroupInfo> findAllByWorkspaceIsNull() {
        return ((LayerGroupRepository) subject).findAllByWorkspaceIsNull();
    }

    public @Override List<LayerGroupInfo> findAllByWorkspace(WorkspaceInfo workspace) {
        return ((LayerGroupRepository) subject).findAllByWorkspace(workspace);
    }
}
