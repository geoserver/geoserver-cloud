/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;

public class CloudStoreRepository extends CatalogServiceClientRepository<StoreInfo>
        implements StoreRepository {

    private final @Getter Class<StoreInfo> infoType = StoreInfo.class;

    public @Override void setDefaultDataStore(
            @NonNull WorkspaceInfo workspace, @NonNull DataStoreInfo dataStore) {

        String workspaceId = workspace.getId();
        String dataStoreId = dataStore.getId();
        blockAndReturn(client().setDefaultDataStoreByWorkspaceId(workspaceId, dataStoreId));
    }

    public @Override void unsetDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        String workspaceId = workspace.getId();
        client().unsetDefaultDataStore(workspaceId).block();
    }

    public @Override Optional<DataStoreInfo> getDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        String workspaceId = workspace.getId();
        return blockAndReturn(client().findDefaultDataStoreByWorkspaceId(workspaceId));
    }

    public @Override Stream<DataStoreInfo> getDefaultDataStores() {
        return client().getDefaultDataStores().toStream().map(this::resolve);
    }

    public @Override <T extends StoreInfo> Stream<T> findAllByWorkspace(
            @NonNull WorkspaceInfo workspace, @Nullable Class<T> clazz) {

        String workspaceId = workspace.getId();
        ClassMappings type = typeEnum(clazz);

        Flux<T> flux = client().findStoresByWorkspaceId(workspaceId, type);
        return flux.toStream().map(this::resolve);
    }

    public @Override <T extends StoreInfo> Stream<T> findAllByType(@NonNull Class<T> clazz) {

        return client().findAll(endpoint(), typeEnum(clazz))
                .map(clazz::cast)
                .map(this::resolve)
                .toStream();
    }

    public @Override <T extends StoreInfo> Optional<T> findByNameAndWorkspace(
            @NonNull String name, @NonNull WorkspaceInfo workspace, @NonNull Class<T> clazz) {

        String workspaceId = workspace.getId();
        ClassMappings type = typeEnum(clazz);
        return blockAndReturn(client().findStoreByWorkspaceIdAndName(workspaceId, name, type));
    }
}
