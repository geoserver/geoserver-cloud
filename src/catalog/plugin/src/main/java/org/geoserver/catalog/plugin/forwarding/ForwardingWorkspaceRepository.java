/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.Optional;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;

/**
 * A decorator for {@link WorkspaceRepository} that forwards all method calls to an underlying repository.
 *
 * <p>This class extends {@link ForwardingCatalogRepository} to wrap a {@link WorkspaceRepository} subject,
 * delegating all operations related to {@link WorkspaceInfo} management. It enables subclasses to override
 * specific methods to customize behavior (e.g., adding validation or logging) without modifying the core
 * repository implementation.
 *
 * @since 1.0
 * @see WorkspaceRepository
 * @see ForwardingCatalogRepository
 */
public class ForwardingWorkspaceRepository extends ForwardingCatalogRepository<WorkspaceInfo, WorkspaceRepository>
        implements WorkspaceRepository {

    /**
     * Constructs a forwarding workspace repository wrapping the provided subject.
     *
     * @param subject The underlying {@link WorkspaceRepository} to forward calls to; may be null (behavior depends on subclass).
     */
    public ForwardingWorkspaceRepository(WorkspaceRepository subject) {
        super(subject);
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        subject.setDefaultWorkspace(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<WorkspaceInfo> getDefaultWorkspace() {
        return subject.getDefaultWorkspace();
    }

    /** {@inheritDoc} */
    @Override
    public void unsetDefaultWorkspace() {
        subject.unsetDefaultWorkspace();
    }
}
