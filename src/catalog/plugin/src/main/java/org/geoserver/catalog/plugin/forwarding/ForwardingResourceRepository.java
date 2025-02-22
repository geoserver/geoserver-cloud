/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;

/**
 * A decorator for {@link ResourceRepository} that forwards all method calls to an underlying repository.
 *
 * <p>This class extends {@link ForwardingCatalogRepository} to wrap a {@link ResourceRepository} subject,
 * delegating all operations related to {@link ResourceInfo} management. It enables subclasses to override
 * specific methods to customize behavior (e.g., adding validation or logging) without modifying the core
 * repository implementation.
 *
 * @since 1.0
 * @see ResourceRepository
 * @see ForwardingCatalogRepository
 */
public class ForwardingResourceRepository extends ForwardingCatalogRepository<ResourceInfo, ResourceRepository>
        implements ResourceRepository {

    /**
     * Constructs a forwarding resource repository wrapping the provided subject.
     *
     * @param subject The underlying {@link ResourceRepository} to forward calls to; may be null (behavior depends on subclass).
     */
    public ForwardingResourceRepository(ResourceRepository subject) {
        super(subject);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> Stream<T> findAllByType(Class<T> clazz) {
        return subject.findAllByType(clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> Stream<T> findAllByNamespace(NamespaceInfo ns, Class<T> clazz) {
        return subject.findAllByNamespace(ns, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> Optional<T> findByStoreAndName(StoreInfo store, String name, Class<T> clazz) {
        return subject.findByStoreAndName(store, name, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> Stream<T> findAllByStore(StoreInfo store, Class<T> clazz) {
        return subject.findAllByStore(store, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends ResourceInfo> Optional<T> findByNameAndNamespace(
            @NonNull String name, @NonNull NamespaceInfo namespace, Class<T> clazz) {
        return subject.findByNameAndNamespace(name, namespace, clazz);
    }
}
