/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.List;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;

public class ForwardingLayerRepository
        extends ForwardingCatalogRepository<LayerInfo, LayerRepository> implements LayerRepository {

    public ForwardingLayerRepository(LayerRepository subject) {
        super(subject);
    }

    public @Override LayerInfo findOneByName(String name) {
        return subject.findOneByName(name);
    }

    public @Override List<LayerInfo> findAllByDefaultStyleOrStyles(StyleInfo style) {
        return subject.findAllByDefaultStyleOrStyles(style);
    }

    public @Override List<LayerInfo> findAllByResource(ResourceInfo resource) {
        return subject.findAllByResource(resource);
    }
}
