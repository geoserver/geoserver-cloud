/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.forwarding;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;

/**
 * A decorator for {@link LayerGroupRepository} that forwards all method calls to an underlying repository.
 *
 * <p>This class extends {@link ForwardingCatalogRepository} to wrap a {@link LayerGroupRepository} subject,
 * delegating all operations related to {@link LayerGroupInfo} management. It enables subclasses to override
 * specific methods to customize behavior (e.g., adding validation or logging) without modifying the core
 * repository implementation.
 *
 * @since 1.0
 * @see LayerGroupRepository
 * @see ForwardingCatalogRepository
 */
public class ForwardingLayerGroupRepository extends ForwardingCatalogRepository<LayerGroupInfo, LayerGroupRepository>
        implements LayerGroupRepository {

    /**
     * Constructs a forwarding layer group repository wrapping the provided subject.
     *
     * @param subject The underlying {@link LayerGroupRepository} to forward calls to; may be null (behavior depends on subclass).
     */
    public ForwardingLayerGroupRepository(LayerGroupRepository subject) {
        super(subject);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<LayerGroupInfo> findAllByWorkspaceIsNull() {
        return subject.findAllByWorkspaceIsNull();
    }

    /** {@inheritDoc} */
    @Override
    public Stream<LayerGroupInfo> findAllByWorkspace(WorkspaceInfo workspace) {
        return subject.findAllByWorkspace(workspace);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<LayerGroupInfo> findByNameAndWorkspaceIsNull(@NonNull String name) {
        return subject.findByNameAndWorkspaceIsNull(name);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<LayerGroupInfo> findByNameAndWorkspace(String name, WorkspaceInfo workspace) {
        return subject.findByNameAndWorkspace(name, workspace);
    }
}
