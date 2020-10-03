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

    public @Override void onAfterAdd(Catalog catalog, NamespaceInfo added) {
        if (catalog.getDefaultNamespace() == null) {
            catalog.setDefaultNamespace(added);
        }
    }

    public @Override void onRemoved(Catalog catalog, NamespaceInfo namespace) {
        selectNewDefaultNamespaceIfRemoved(catalog, namespace);
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
