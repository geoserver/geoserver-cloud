/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.Optional;
import java.util.stream.Stream;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;

/**
 * A decorator for {@link LayerRepository} that forwards all method calls to an underlying repository.
 *
 * <p>This class extends {@link ForwardingCatalogRepository} to wrap a {@link LayerRepository} subject,
 * delegating all operations related to {@link LayerInfo} management. It enables subclasses to override
 * specific methods to customize behavior (e.g., adding validation or logging) without modifying the core
 * repository implementation.
 *
 * @since 1.0
 * @see LayerRepository
 * @see ForwardingCatalogRepository
 */
public class ForwardingLayerRepository extends ForwardingCatalogRepository<LayerInfo, LayerRepository>
        implements LayerRepository {

    /**
     * Constructs a forwarding layer repository wrapping the provided subject.
     *
     * @param subject The underlying {@link LayerRepository} to forward calls to; may be null (behavior depends on subclass).
     */
    public ForwardingLayerRepository(LayerRepository subject) {
        super(subject);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<LayerInfo> findOneByName(String name) {
        return subject.findOneByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<LayerInfo> findAllByDefaultStyleOrStyles(StyleInfo style) {
        return subject.findAllByDefaultStyleOrStyles(style);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<LayerInfo> findAllByResource(ResourceInfo resource) {
        return subject.findAllByResource(resource);
    }
}
