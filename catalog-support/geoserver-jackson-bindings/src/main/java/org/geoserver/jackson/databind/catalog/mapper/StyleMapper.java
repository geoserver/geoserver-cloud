/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.mapper;

import org.geoserver.catalog.StyleInfo;
import org.geoserver.jackson.databind.catalog.dto.Style;
import org.mapstruct.Mapper;

@Mapper(config = CatalogInfoMapperConfig.class)
public interface StyleMapper {

    StyleInfo map(Style o);

    Style map(StyleInfo o);
}
