/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.stream.Stream;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient;
import org.springframework.lang.Nullable;
import lombok.Getter;
import lombok.NonNull;

public class CloudStoreRepository extends CatalogServiceClientRepository<StoreInfo>
        implements StoreRepository {

    private final @Getter Class<StoreInfo> infoType = StoreInfo.class;

    protected CloudStoreRepository(@NonNull ReactiveCatalogClient client) {
        super(client);
    }

    public @Override void setDefaultDataStore(@NonNull WorkspaceInfo workspace,
            @NonNull DataStoreInfo dataStore) {
        client().setDefaultDataStoreByWorkspaceId(workspace.getId(), dataStore.getId());
    }

    public @Override @Nullable DataStoreInfo getDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        return client().findDefaultDataStoreByWorkspaceId(workspace.getId()).block();
    }

    public @Override Stream<DataStoreInfo> getDefaultDataStores() {
        return client().getDefaultDataStores().toStream();
    }

    @SuppressWarnings("unchecked")
    public @Override <T extends StoreInfo> Stream<T> findAllByWorkspace(
            @NonNull WorkspaceInfo workspace, @Nullable Class<T> clazz) {
        return (Stream<T>) client().findStoresByWorkspaceId(workspace.getId(), typeEnum(clazz));
    }

    public @Override <T extends StoreInfo> Stream<T> findAllByType(@NonNull Class<T> clazz) {
        return client().findAll(typeEnum(clazz)).map(clazz::cast).toStream();
    }

    public @Override <T extends StoreInfo> T findByNameAndWorkspace(@NonNull String name,
            @NonNull WorkspaceInfo workspace, @NonNull Class<T> clazz) {
        return client().findStoreByWorkspaceIdAndName(name, workspace.getId(), typeEnum(clazz))
                .map(clazz::cast).block();
    }
}
