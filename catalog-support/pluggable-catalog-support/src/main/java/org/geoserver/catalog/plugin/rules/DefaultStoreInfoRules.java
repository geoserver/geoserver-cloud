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

    public @Override void onAfterAdd(Catalog catalog, StoreInfo store) {
        if (store instanceof DataStoreInfo) {
            setAsDefaultIfThereIsNoDefaultForItsWorkspace(catalog, (DataStoreInfo) store);
        }
    }

    public @Override void onRemoved(Catalog catalog, StoreInfo store) {
        if (store instanceof DataStoreInfo) {
            selectNewDefaultDataStoreIfRemoved(catalog, (DataStoreInfo) store);
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
            // TODO: this will fire multiple events, we want to fire only one
            catalog.setDefaultDataStore(workspace, null);

            // default removed, choose another store to become default if possible
            List<DataStoreInfo> dstores = catalog.getDataStoresByWorkspace(workspace);
            if (!dstores.isEmpty()) {
                DataStoreInfo newDefault = dstores.get(0);
                catalog.setDefaultDataStore(workspace, newDefault);
            }
        }
    }
}
