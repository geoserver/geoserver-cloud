/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.rules;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;

import java.util.List;

/** Encapsulates default {@link Catalog} business rules for {@link StoreInfo} objects */
public class DefaultStoreInfoRules implements CatalogInfoBusinessRules<StoreInfo> {

    private static final String SET_DEFAULT = "set_default_datastore";
    private static final String SELECT_NEW_DEFAULT_FOR_WORKSPACE = "select_new_default_datastore";

    /**
     * Determines whether there's no current default datastore and stores a flag in {@code context}
     * for {@link #afterAdd} to proceed if so. This allows to improve consistency for {@code
     * Catalog.add(StoreInfo)} in situations where there are multiple catalogs: if there was no
     * default data store when the method was entered, then the created one is set as the default
     * when the method returns.
     */
    public @Override void beforeAdd(CatalogOpContext<StoreInfo> context) {
        if (context.getObject() instanceof DataStoreInfo) {
            WorkspaceInfo workspace = context.getObject().getWorkspace();
            Catalog catalog = context.getCatalog();
            context.set(SET_DEFAULT, () -> null == catalog.getDefaultDataStore(workspace));
        }
    }

    /**
     * If the context object is a {@link DataStoreInfo} and its workspace has no default datastore,
     * sets it as the workspace's default data store.
     */
    public @Override void afterAdd(CatalogOpContext<StoreInfo> context) {
        if (context.getObject() instanceof DataStoreInfo) {
            setAsDefaultDataStoreInWorkspace(context.as(DataStoreInfo.class));
        }
    }

    public @Override void beforeRemove(CatalogOpContext<StoreInfo> context) {
        if (context.getObject() instanceof DataStoreInfo) {
            StoreInfo toRemove = context.getObject();
            WorkspaceInfo workspace = toRemove.getWorkspace();
            DataStoreInfo defaultDs = context.getCatalog().getDefaultDataStore(workspace);
            boolean needsToSelectNew =
                    defaultDs == null || toRemove.getId().equals(defaultDs.getId());
            if (needsToSelectNew) {
                context.setContextOption(SELECT_NEW_DEFAULT_FOR_WORKSPACE, workspace);
            }
        }
    }

    /**
     * If the operation was successful and as result the store's workspace ends up with no default
     * datastore, establishes a new default datastore for that workspace, if there's some datastore
     * remaining, otherwise sets the workspace's default datastore to {@code null}
     */
    public @Override void afterRemove(CatalogOpContext<StoreInfo> context) {
        if (context.getObject() instanceof DataStoreInfo) {
            selectNewDefaultDataStoreIfRemoved(context.as(DataStoreInfo.class));
        }
    }

    private void setAsDefaultDataStoreInWorkspace(CatalogOpContext<DataStoreInfo> context) {
        if (context.isSuccess() && context.is(SET_DEFAULT)) {
            Catalog catalog = context.getCatalog();
            DataStoreInfo dataStoreInfo = context.getObject();
            WorkspaceInfo workspace = dataStoreInfo.getWorkspace();
            catalog.setDefaultDataStore(workspace, dataStoreInfo);
        }
    }

    private void selectNewDefaultDataStoreIfRemoved(CatalogOpContext<DataStoreInfo> context) {
        WorkspaceInfo needsToSelectNew = context.getContextOption(SELECT_NEW_DEFAULT_FOR_WORKSPACE);
        if (!context.isSuccess() || needsToSelectNew == null) {
            return;
        }
        Catalog catalog = context.getCatalog();
        WorkspaceInfo workspace = catalog.getWorkspace(needsToSelectNew.getId());
        if (workspace == null) {
            return;
        }
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
