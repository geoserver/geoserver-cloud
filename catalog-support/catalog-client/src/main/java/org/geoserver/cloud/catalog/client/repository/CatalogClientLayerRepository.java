/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import lombok.Getter;
import lombok.NonNull;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;

import java.util.Optional;
import java.util.stream.Stream;

public class CatalogClientLayerRepository extends CatalogClientRepository<LayerInfo>
        implements LayerRepository {

    private final @Getter Class<LayerInfo> contentType = LayerInfo.class;

    public @Override Stream<LayerInfo> findAllByDefaultStyleOrStyles(StyleInfo style) {
        return toStream(client().findLayersWithStyle(style.getId()));
    }

    public @Override Stream<LayerInfo> findAllByResource(ResourceInfo resource) {
        return toStream(client().findLayersByResourceId(resource.getId()));
    }

    public @Override Optional<LayerInfo> findOneByName(@NonNull String name) {
        return findFirstByName(name, LayerInfo.class);
    }
}
