/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.mapper;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.jackson.databind.catalog.dto.Layer;
import org.geoserver.jackson.databind.catalog.dto.LayerGroup;
import org.geoserver.jackson.databind.catalog.dto.Published;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = SpringCatalogInfoMapperConfig.class)
public interface PublishedMapper {

    default PublishedInfo map(Published dto) {
        if (dto == null) return null;
        if (dto instanceof Layer) return map((Layer) dto);
        if (dto instanceof LayerGroup) return map((LayerGroup) dto);

        throw new IllegalArgumentException(
                "Unknown Published type: " + dto.getClass().getCanonicalName());
    }

    default Published map(PublishedInfo info) {
        if (info == null) return null;
        if (info instanceof LayerInfo) return map((LayerInfo) info);
        if (info instanceof LayerGroupInfo) return map((LayerGroupInfo) info);

        throw new IllegalArgumentException(
                "Unknown PublishedInfo type: " + info.getClass().getCanonicalName());
    }

    // it's about time for the infamous resource/publish split isn't it?
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "abstract", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "advertised", ignore = true)
    LayerInfo map(Layer o);

    Layer map(LayerInfo o);

    LayerGroupInfo map(LayerGroup o);

    LayerGroup map(LayerGroupInfo o);
}
