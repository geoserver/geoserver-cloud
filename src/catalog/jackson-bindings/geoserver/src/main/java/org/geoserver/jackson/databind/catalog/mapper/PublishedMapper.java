/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.mapper;

import lombok.Generated;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.jackson.databind.catalog.dto.Layer;
import org.geoserver.jackson.databind.catalog.dto.LayerGroup;
import org.geoserver.jackson.databind.catalog.dto.Published;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CatalogInfoMapperConfig.class)
@AnnotateWith(value = Generated.class)
public interface PublishedMapper {

    default PublishedInfo map(Published dto) {
        if (dto == null) return null;
        if (dto instanceof Layer l) return map(l);
        if (dto instanceof LayerGroup lg) return map(lg);

        throw new IllegalArgumentException(
                "Unknown Published type: " + dto.getClass().getCanonicalName());
    }

    default Published map(PublishedInfo info) {
        if (info == null) return null;
        if (info instanceof LayerInfo l) return map(l);
        if (info instanceof LayerGroupInfo lg) return map(lg);

        throw new IllegalArgumentException(
                "Unknown PublishedInfo type: " + info.getClass().getCanonicalName());
    }

    // it's about time for the infamous resource/publish split isn't it?
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "abstract", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "advertised", ignore = true)
    @Mapping(target = "internationalTitle", ignore = true)
    @Mapping(target = "internationalAbstract", ignore = true)
    @Mapping(target = "resource", source = "resource", qualifiedByName = "resourceInfo")
    LayerInfo map(Layer o);

    Layer map(LayerInfo o);

    @Mapping(source = "layers", target = "layers", qualifiedByName = "publishedInfo")
    LayerGroupInfo map(LayerGroup o);

    LayerGroup map(LayerGroupInfo o);

    @Mapping(source = "layers", target = "layers", qualifiedByName = "publishedInfo")
    LayerGroupStyle map(org.geoserver.jackson.databind.catalog.dto.LayerGroupStyle o);

    org.geoserver.jackson.databind.catalog.dto.LayerGroupStyle map(LayerGroupStyle o);
}
