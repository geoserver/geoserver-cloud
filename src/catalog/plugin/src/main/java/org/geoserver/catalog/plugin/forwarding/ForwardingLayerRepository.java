/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;

import java.util.Optional;
import java.util.stream.Stream;

public class ForwardingLayerRepository
        extends ForwardingCatalogRepository<LayerInfo, LayerRepository> implements LayerRepository {

    public ForwardingLayerRepository(LayerRepository subject) {
        super(subject);
    }

    @Override
    public Optional<LayerInfo> findOneByName(String name) {
        return subject.findOneByName(name);
    }

    @Override
    public Stream<LayerInfo> findAllByDefaultStyleOrStyles(StyleInfo style) {
        return subject.findAllByDefaultStyleOrStyles(style);
    }

    @Override
    public Stream<LayerInfo> findAllByResource(ResourceInfo resource) {
        return subject.findAllByResource(resource);
    }
}
