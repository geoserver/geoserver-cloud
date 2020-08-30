/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;

public class ForwardingWorkspaceRepository extends ForwardingCatalogRepository<WorkspaceInfo>
        implements WorkspaceRepository {

    public ForwardingWorkspaceRepository(CatalogInfoRepository<WorkspaceInfo> subject) {
        super(subject);
    }

    public @Override void setDefaultWorkspace(WorkspaceInfo workspace) {
        ((WorkspaceRepository) subject).setDefaultWorkspace(workspace);
    }

    public @Override WorkspaceInfo getDefaultWorkspace() {
        return ((WorkspaceRepository) subject).getDefaultWorkspace();
    }
}
