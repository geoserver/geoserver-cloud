/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.springframework.lang.Nullable;

public class CloudStoreRepository extends CatalogServiceClientRepository<StoreInfo>
        implements StoreRepository {

    private final @Getter Class<StoreInfo> infoType = StoreInfo.class;

    public @Override void setDefaultDataStore(
            @NonNull WorkspaceInfo workspace, @NonNull DataStoreInfo dataStore) {

        callAndBlock(
                () ->
                        client().setDefaultDataStoreByWorkspaceId(
                                        workspace.getId(), dataStore.getId()));
    }

    public @Override @Nullable DataStoreInfo getDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        return callAndReturn(() -> client().findDefaultDataStoreByWorkspaceId(workspace.getId()));
    }

    public @Override Stream<DataStoreInfo> getDefaultDataStores() {
        return client().getDefaultDataStores().map(this::resolve).toStream();
    }

    public @Override <T extends StoreInfo> Stream<T> findAllByWorkspace(
            @NonNull WorkspaceInfo workspace, @Nullable Class<T> clazz) {

        return client().findStoresByWorkspaceId(workspace.getId(), typeEnum(clazz))
                .map(clazz::cast)
                .map(this::resolve)
                .toStream();
    }

    public @Override <T extends StoreInfo> Stream<T> findAllByType(@NonNull Class<T> clazz) {
        return client().findAll(endpoint(), typeEnum(clazz))
                .map(clazz::cast)
                .map(this::resolve)
                .toStream();
    }

    public @Override <T extends StoreInfo> T findByNameAndWorkspace(
            @NonNull String name, @NonNull WorkspaceInfo workspace, @NonNull Class<T> clazz) {
        return callAndReturn(
                () ->
                        client().findStoreByWorkspaceIdAndName(
                                        workspace.getId(), name, typeEnum(clazz))
                                .map(clazz::cast));
    }
}
