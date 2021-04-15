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

    private static final String SET_DEFAULT = "set_default_workspace";

    /**
     * Determines whether there's no current default workspace and stores it as a flag in {@code
     * context} for {@link #afterAdd} to proceed if so. This allows to improve consistency for
     * {@code Catalog.add(WorkspaceInfo)} in situations where there are multiple catalogs: if there
     * was no default workspace when the method was entered, then the created one is set as the
     * default when the method returns.
     */
    public @Override void beforeAdd(CatalogOpContext<WorkspaceInfo> context) {
        WorkspaceInfo defaultWorkspace = context.getCatalog().getDefaultWorkspace();
        Boolean needsSetDefault = defaultWorkspace == null;
        context.setContextOption(SET_DEFAULT, needsSetDefault);
    }

    /**
     * Sets the created workspace as the catalog's default workspace if so determined in {@link
     * #beforeAdd} and the operation was successful.
     */
    public @Override void afterAdd(CatalogOpContext<WorkspaceInfo> context) {
        // there's only this rule so far:
        setAsDefaultIfThereWasNoDefaultWorkspace(context);
    }

    /**
     * Selects a new catalog default workspace if as the result of removing the workspace referred
     * to by {@code context.getObject()}, the catalog has no default workspace.
     */
    public @Override void afterRemove(CatalogOpContext<WorkspaceInfo> context) {
        if (context.isSuccess()) {
            selectNewDefaultWorkspaceIfRemoved(context.getCatalog(), context.getObject());
        }
    }

    private void setAsDefaultIfThereWasNoDefaultWorkspace(CatalogOpContext<WorkspaceInfo> context) {
        if (context.isSuccess()) {
            Boolean needsSetDefault = context.getContextOption(SET_DEFAULT);
            if (needsSetDefault.booleanValue()) {
                context.getCatalog().setDefaultWorkspace(context.getObject());
            }
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
