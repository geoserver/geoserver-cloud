/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import lombok.NonNull;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;

import java.util.Optional;
import java.util.stream.Stream;

public class ForwardingStoreRepository
        extends ForwardingCatalogRepository<StoreInfo, StoreRepository> implements StoreRepository {

    public ForwardingStoreRepository(StoreRepository subject) {
        super(subject);
    }

    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo dataStore) {
        subject.setDefaultDataStore(workspace, dataStore);
    }

    @Override
    public Optional<DataStoreInfo> getDefaultDataStore(WorkspaceInfo workspace) {
        return subject.getDefaultDataStore(workspace);
    }

    @Override
    public Stream<DataStoreInfo> getDefaultDataStores() {
        return subject.getDefaultDataStores();
    }

    @Override
    public <T extends StoreInfo> Stream<T> findAllByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        return subject.findAllByWorkspace(workspace, clazz);
    }

    @Override
    public <T extends StoreInfo> Stream<T> findAllByType(Class<T> clazz) {
        return subject.findAllByType(clazz);
    }

    @Override
    public <T extends StoreInfo> Optional<T> findByNameAndWorkspace(
            String name, WorkspaceInfo workspace, Class<T> clazz) {
        return subject.findByNameAndWorkspace(name, workspace, clazz);
    }

    @Override
    public void unsetDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        subject.unsetDefaultDataStore(workspace);
    }
}
