/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.rules;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;

/** Encapsulates default {@link Catalog} business rules for {@link StoreInfo} objects */
public class DefaultStoreInfoRules implements CatalogInfoBusinessRules<StoreInfo> {

    /**
     * If the context object is a {@link DataStoreInfo} and its workspace has no default datastore,
     * sets it as the workspace's default data store.
     */
    public @Override void afterAdd(CatalogOpContext<StoreInfo> context) {
        if (context.isSuccess() && context.getObject() instanceof DataStoreInfo) {
            setAsDefaultIfThereIsNoDefaultForItsWorkspace(
                    context.getCatalog(), (DataStoreInfo) context.getObject());
        }
    }

    /**
     * If the operation was successful and as result the store's workspace ends up with no default
     * datastore, establishes a new default datastore for that workspace, if there's some datastore
     * remaining, otherwise sets the workspace's default datastore to {@code null}
     */
    public @Override void afterRemove(CatalogOpContext<StoreInfo> context) {
        if (context.isSuccess() && context.getObject() instanceof DataStoreInfo) {
            selectNewDefaultDataStoreIfRemoved(
                    context.getCatalog(), (DataStoreInfo) context.getObject());
        }
    }

    private void setAsDefaultIfThereIsNoDefaultForItsWorkspace(
            Catalog catalog, DataStoreInfo store) {

        WorkspaceInfo workspace = store.getWorkspace();
        DataStoreInfo defaultDataStore = catalog.getDefaultDataStore(workspace);
        if (defaultDataStore == null) {
            catalog.setDefaultDataStore(workspace, store);
        }
    }

    private void selectNewDefaultDataStoreIfRemoved(Catalog catalog, DataStoreInfo store) {
        WorkspaceInfo workspace = store.getWorkspace();
        DataStoreInfo defaultStore = catalog.getDefaultDataStore(workspace);
        if (defaultStore == null || store.getId().equals(defaultStore.getId())) {
            // default removed, choose another store to become default if possible
            List<DataStoreInfo> dstores = catalog.getDataStoresByWorkspace(workspace);
            if (dstores.isEmpty()) {
                catalog.setDefaultDataStore(workspace, null);
            } else {
                DataStoreInfo newDefault = dstores.get(0);
                catalog.setDefaultDataStore(workspace, newDefault);
            }
        }
    }
}
