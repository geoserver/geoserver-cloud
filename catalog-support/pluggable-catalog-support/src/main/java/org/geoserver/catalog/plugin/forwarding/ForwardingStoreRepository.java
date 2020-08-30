/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.List;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;

public class ForwardingStoreRepository extends ForwardingCatalogRepository<StoreInfo>
        implements StoreRepository {

    public ForwardingStoreRepository(StoreRepository subject) {
        super(subject);
    }

    public @Override void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo dataStore) {
        ((StoreRepository) subject).setDefaultDataStore(workspace, dataStore);
    }

    public @Override DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return ((StoreRepository) subject).getDefaultDataStore(workspace);
    }

    public @Override List<DataStoreInfo> getDefaultDataStores() {
        return ((StoreRepository) subject).getDefaultDataStores();
    }

    public @Override <T extends StoreInfo> T findOneByName(String name, Class<T> clazz) {
        return ((StoreRepository) subject).findOneByName(name, clazz);
    }

    public @Override <T extends StoreInfo> List<T> findAllByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        return ((StoreRepository) subject).findAllByWorkspace(workspace, clazz);
    }

    public @Override <T extends StoreInfo> List<T> findAllByType(Class<T> clazz) {
        return ((StoreRepository) subject).findAllByType(clazz);
    }
}
