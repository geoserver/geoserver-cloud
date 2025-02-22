/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.Optional;
import java.util.stream.Stream;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;

/**
 * A decorator for {@link NamespaceRepository} that forwards all method calls to an underlying repository.
 *
 * <p>This class extends {@link ForwardingCatalogRepository} to wrap a {@link NamespaceRepository} subject,
 * delegating all operations related to {@link NamespaceInfo} management. It enables subclasses to override
 * specific methods to customize behavior (e.g., adding validation or logging) without modifying the core
 * repository implementation.
 *
 * @since 1.0
 * @see NamespaceRepository
 * @see ForwardingCatalogRepository
 */
public class ForwardingNamespaceRepository extends ForwardingCatalogRepository<NamespaceInfo, NamespaceRepository>
        implements NamespaceRepository {

    /**
     * Constructs a forwarding namespace repository wrapping the provided subject.
     *
     * @param subject The underlying {@link NamespaceRepository} to forward calls to; may be null (behavior depends on subclass).
     */
    public ForwardingNamespaceRepository(NamespaceRepository subject) {
        super(subject);
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultNamespace(NamespaceInfo namespace) {
        subject.setDefaultNamespace(namespace);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<NamespaceInfo> getDefaultNamespace() {
        return subject.getDefaultNamespace();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<NamespaceInfo> findOneByURI(String uri) {
        return subject.findOneByURI(uri);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<NamespaceInfo> findAllByURI(String uri) {
        return subject.findAllByURI(uri);
    }

    /** {@inheritDoc} */
    @Override
    public void unsetDefaultNamespace() {
        subject.unsetDefaultNamespace();
    }
}
