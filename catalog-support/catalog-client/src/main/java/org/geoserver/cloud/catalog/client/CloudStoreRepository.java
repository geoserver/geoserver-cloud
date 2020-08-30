/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client;

import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.cloud.catalog.client.feign.StoreClient;

public class CloudStoreRepository extends CatalogServiceClientRepository<StoreInfo, StoreClient>
        implements StoreRepository {

    private final @Getter Class<StoreInfo> infoType = StoreInfo.class;

    protected CloudStoreRepository(@NonNull StoreClient client) {
        super(client);
    }

    public @Override void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo dataStore) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override List<DataStoreInfo> getDefaultDataStores() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override <T extends StoreInfo> T findOneByName(String name, Class<T> clazz) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override <T extends StoreInfo> List<T> findAllByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public @Override <T extends StoreInfo> List<T> findAllByType(Class<T> clazz) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
