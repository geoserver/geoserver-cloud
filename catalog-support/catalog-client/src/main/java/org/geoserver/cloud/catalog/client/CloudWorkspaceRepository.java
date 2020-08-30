/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client;

import lombok.Getter;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.geoserver.cloud.catalog.client.feign.WorkspaceClient;

public class CloudWorkspaceRepository
        extends CatalogServiceClientRepository<WorkspaceInfo, WorkspaceClient>
        implements WorkspaceRepository {

    private final @Getter Class<WorkspaceInfo> infoType = WorkspaceInfo.class;

    public CloudWorkspaceRepository(WorkspaceClient client) {
        super(client);
    }

    public @Override void setDefaultWorkspace(WorkspaceInfo workspace) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override WorkspaceInfo getDefaultWorkspace() {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
