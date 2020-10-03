/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.rules;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;

/** Encapsulates default {@link Catalog} business rules for {@link WorkspaceInfo} objects */
public class DefaultWorkspaceInfoRules implements CatalogInfoBusinessRules<WorkspaceInfo> {

    public @Override void onAfterAdd(Catalog catalog, WorkspaceInfo workspace) {
        setAsDefaultIfThereIsNoDefaultWorkspace(catalog, workspace);
    }

    public @Override void onRemoved(Catalog catalog, WorkspaceInfo workspace) {
        selectNewDefaultWorkspaceIfRemoved(catalog, workspace);
    }

    private void setAsDefaultIfThereIsNoDefaultWorkspace(Catalog catalog, WorkspaceInfo workspace) {
        // if there is no default workspace use this one as the default
        WorkspaceInfo defaultWorkspace = catalog.getDefaultWorkspace();
        if (defaultWorkspace == null) {
            catalog.setDefaultWorkspace(workspace);
        }
    }

    protected void selectNewDefaultWorkspaceIfRemoved(Catalog catalog, WorkspaceInfo workspace) {
        WorkspaceInfo defaultWorkspace = catalog.getDefaultWorkspace();
        if (defaultWorkspace == null || workspace.getId().equals(defaultWorkspace.getId())) {
            List<WorkspaceInfo> workspaces = catalog.getWorkspaces();
            defaultWorkspace = workspaces.isEmpty() ? null : workspaces.get(0);
            catalog.setDefaultWorkspace(defaultWorkspace);
            if (defaultWorkspace != null) {
                NamespaceInfo defaultNamespace =
                        catalog.getNamespaceByPrefix(defaultWorkspace.getName());
                if (defaultNamespace != null) {
                    catalog.setDefaultNamespace(defaultNamespace);
                }
            }
        }
    }
}
