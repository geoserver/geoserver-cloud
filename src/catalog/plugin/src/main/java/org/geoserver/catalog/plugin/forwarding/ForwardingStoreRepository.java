/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.forwarding;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;

/**
 * A decorator for {@link StoreRepository} that forwards all method calls to an underlying repository.
 *
 * <p>This class extends {@link ForwardingCatalogRepository} to wrap a {@link StoreRepository} subject,
 * delegating all operations related to {@link StoreInfo} management. It enables subclasses to override
 * specific methods to customize behavior (e.g., adding validation or logging) without modifying the core
 * repository implementation.
 *
 * @since 1.0
 * @see StoreRepository
 * @see ForwardingCatalogRepository
 */
public class ForwardingStoreRepository extends ForwardingCatalogRepository<StoreInfo, StoreRepository>
        implements StoreRepository {

    /**
     * Constructs a forwarding store repository wrapping the provided subject.
     *
     * @param subject The underlying {@link StoreRepository} to forward calls to; may be null (behavior depends on subclass).
     */
    public ForwardingStoreRepository(StoreRepository subject) {
        super(subject);
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo dataStore) {
        subject.setDefaultDataStore(workspace, dataStore);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<DataStoreInfo> getDefaultDataStore(WorkspaceInfo workspace) {
        return subject.getDefaultDataStore(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<DataStoreInfo> getDefaultDataStores() {
        return subject.getDefaultDataStores();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> Stream<T> findAllByWorkspace(WorkspaceInfo workspace, Class<T> clazz) {
        return subject.findAllByWorkspace(workspace, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> Stream<T> findAllByType(Class<T> clazz) {
        return subject.findAllByType(clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends StoreInfo> Optional<T> findByNameAndWorkspace(
            String name, WorkspaceInfo workspace, Class<T> clazz) {
        return subject.findByNameAndWorkspace(name, workspace, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public void unsetDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        subject.unsetDefaultDataStore(workspace);
    }
}
