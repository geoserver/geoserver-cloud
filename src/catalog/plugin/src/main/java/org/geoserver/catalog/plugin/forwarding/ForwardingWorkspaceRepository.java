/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.Optional;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;

public class ForwardingWorkspaceRepository extends ForwardingCatalogRepository<WorkspaceInfo, WorkspaceRepository>
        implements WorkspaceRepository {

    public ForwardingWorkspaceRepository(WorkspaceRepository subject) {
        super(subject);
    }

    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        subject.setDefaultWorkspace(workspace);
    }

    @Override
    public Optional<WorkspaceInfo> getDefaultWorkspace() {
        return subject.getDefaultWorkspace();
    }

    @Override
    public void unsetDefaultWorkspace() {
        subject.unsetDefaultWorkspace();
    }
}
