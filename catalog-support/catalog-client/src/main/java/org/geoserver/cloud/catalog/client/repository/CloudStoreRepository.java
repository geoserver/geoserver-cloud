/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.cloud.catalog.client.feign.StoreClient;
import org.springframework.lang.Nullable;

public class CloudStoreRepository extends CatalogServiceClientRepository<StoreInfo, StoreClient>
        implements StoreRepository {

    private final @Getter Class<StoreInfo> infoType = StoreInfo.class;

    protected CloudStoreRepository(@NonNull StoreClient client) {
        super(client);
    }

    public @Override void setDefaultDataStore(
            @NonNull WorkspaceInfo workspace, @NonNull DataStoreInfo dataStore) {
        client().setDefaultDataStoreByWorkspaceId(workspace.getId(), dataStore.getId());
    }

    public @Override @Nullable DataStoreInfo getDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        return client().findDefaultDataStoreByWorkspaceId(workspace.getId());
    }

    public @Override List<DataStoreInfo> getDefaultDataStores() {
        return client().getDefaultDataStores();
    }

    @SuppressWarnings("unchecked")
    public @Override <T extends StoreInfo> List<T> findAllByWorkspace(
            @NonNull WorkspaceInfo workspace, @Nullable Class<T> clazz) {
        return (List<T>) client().findAllByWorkspaceId(workspace.getId(), typeEnum(clazz));
    }

    @SuppressWarnings("unchecked")
    public @Override <T extends StoreInfo> List<T> findAllByType(@Nullable Class<T> clazz) {
        return (List<T>) client().findAllByType(typeEnum(clazz));
    }

    public @Override <T extends StoreInfo> T findByNameAndWorkspace(
            @NonNull String name, @NonNull WorkspaceInfo workspace, @NonNull Class<T> clazz) {
        return clazz.cast(
                client().findByNameAndWorkspaceId(name, workspace.getId(), typeEnum(clazz)));
    }
}
