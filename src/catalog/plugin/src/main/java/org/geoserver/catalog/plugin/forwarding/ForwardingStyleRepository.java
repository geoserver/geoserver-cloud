/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.Optional;
import java.util.stream.Stream;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;

/**
 * A decorator for {@link StyleRepository} that forwards all method calls to an underlying repository.
 *
 * <p>This class extends {@link ForwardingCatalogRepository} to wrap a {@link StyleRepository} subject,
 * delegating all operations related to {@link StyleInfo} management. It enables subclasses to override
 * specific methods to customize behavior (e.g., adding validation or logging) without modifying the core
 * repository implementation.
 *
 * @since 1.0
 * @see StyleRepository
 * @see ForwardingCatalogRepository
 */
public class ForwardingStyleRepository extends ForwardingCatalogRepository<StyleInfo, StyleRepository>
        implements StyleRepository {

    /**
     * Constructs a forwarding style repository wrapping the provided subject.
     *
     * @param subject The underlying {@link StyleRepository} to forward calls to; may be null (behavior depends on subclass).
     */
    public ForwardingStyleRepository(StyleRepository subject) {
        super(subject);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<StyleInfo> findAllByNullWorkspace() {
        return subject.findAllByNullWorkspace();
    }

    /** {@inheritDoc} */
    @Override
    public Stream<StyleInfo> findAllByWorkspace(WorkspaceInfo ws) {
        return subject.findAllByWorkspace(ws);
    }

    /**
     * {@inheritDoc}
     * <p>Note: Assuming "Wordkspace" is a typo for "Workspace" in the method name.
     */
    @Override
    public Optional<StyleInfo> findByNameAndWordkspaceNull(String name) {
        return subject.findByNameAndWordkspaceNull(name);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<StyleInfo> findByNameAndWorkspace(String name, WorkspaceInfo workspace) {
        return subject.findByNameAndWorkspace(name, workspace);
    }
}
