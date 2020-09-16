/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.mapper;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.jackson.databind.catalog.dto.CatalogInfoDto;
import org.mapstruct.Mapper;

@Mapper(config = SpringCatalogInfoMapperConfig.class)
public interface CatalogInfoMapper {

    default CatalogInfo map(CatalogInfoDto dto) {
        throw new UnsupportedOperationException("implement!");
    }

    default CatalogInfoDto map(CatalogInfo info) {
        throw new UnsupportedOperationException("implement!");
    }
}
