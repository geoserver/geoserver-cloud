/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.rules;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;

/** Encapsulates default {@link Catalog} business rules for {@link NamespaceInfo} objects */
public class DefaultNamespaceInfoRules implements CatalogInfoBusinessRules<NamespaceInfo> {

    private static final String SET_DEFAULT = "set_default_namespace";

    /**
     * Determines whether there's no current default namespace and stores it as a flag in {@code
     * context} for {@link #afterAdd} to proceed if so. This allows to improve consistency for
     * {@code Catalog.add(NamespaceInfo)} in situations where there are multiple catalogs: if there
     * was no default namespace when the method was entered, then the created one is set as the
     * default when the method returns.
     */
    public @Override void beforeAdd(CatalogOpContext<NamespaceInfo> context) {
        NamespaceInfo defaultNamespace = context.getCatalog().getDefaultNamespace();
        Boolean needsSetDefault = defaultNamespace == null;
        context.setContextOption(SET_DEFAULT, needsSetDefault);
    }

    /**
     * Sets the created namespace as the catalog's default namespace if so determined in {@link
     * #beforeAdd} and the operation was successful.
     */
    public @Override void afterAdd(CatalogOpContext<NamespaceInfo> context) {
        if (context.isSuccess()) {
            Boolean needsSetDefault = context.getContextOption(SET_DEFAULT);
            if (needsSetDefault.booleanValue()) {
                context.getCatalog().setDefaultNamespace(context.getObject());
            }
        }
    }

    /**
     * Selects a new catalog default namespace if as the result of removing the namespace refered to
     * by {@code context.getObject()}, the catalog has no default one.
     */
    public @Override void afterRemove(CatalogOpContext<NamespaceInfo> context) {
        if (context.isSuccess()) {
            selectNewDefaultNamespaceIfRemoved(context.getCatalog(), context.getObject());
        }
    }

    protected void selectNewDefaultNamespaceIfRemoved(Catalog catalog, NamespaceInfo namespace) {
        NamespaceInfo defaultNamespace = catalog.getDefaultNamespace();
        if (defaultNamespace == null || namespace.getId().equals(defaultNamespace.getId())) {
            List<NamespaceInfo> namespaces = catalog.getNamespaces();
            defaultNamespace = namespaces.isEmpty() ? null : namespaces.get(0);
            catalog.setDefaultNamespace(defaultNamespace);
            if (defaultNamespace != null) {
                WorkspaceInfo defaultWorkspace =
                        catalog.getWorkspaceByName(defaultNamespace.getPrefix());
                if (defaultWorkspace != null) {
                    catalog.setDefaultWorkspace(defaultWorkspace);
                }
            }
        }
    }
}
