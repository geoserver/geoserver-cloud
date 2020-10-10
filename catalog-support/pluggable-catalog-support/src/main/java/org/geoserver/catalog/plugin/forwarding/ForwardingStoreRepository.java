/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;

public class ForwardingStoreRepository
        extends ForwardingCatalogRepository<StoreInfo, StoreRepository> implements StoreRepository {

    public ForwardingStoreRepository(StoreRepository subject) {
        super(subject);
    }

    public @Override void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo dataStore) {
        subject.setDefaultDataStore(workspace, dataStore);
    }

    public @Override Optional<DataStoreInfo> getDefaultDataStore(WorkspaceInfo workspace) {
        return subject.getDefaultDataStore(workspace);
    }

    public @Override Stream<DataStoreInfo> getDefaultDataStores() {
        return subject.getDefaultDataStores();
    }

    public @Override <T extends StoreInfo> Stream<T> findAllByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        return subject.findAllByWorkspace(workspace, clazz);
    }

    public @Override <T extends StoreInfo> Stream<T> findAllByType(Class<T> clazz) {
        return subject.findAllByType(clazz);
    }

    public @Override <T extends StoreInfo> Optional<T> findByNameAndWorkspace(
            String name, WorkspaceInfo workspace, Class<T> clazz) {
        return subject.findByNameAndWorkspace(name, workspace, clazz);
    }

    public @Override void unsetDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        subject.unsetDefaultDataStore(workspace);
    }
}
