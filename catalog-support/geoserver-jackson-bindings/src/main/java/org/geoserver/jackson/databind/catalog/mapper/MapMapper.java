package org.geoserver.jackson.databind.catalog.mapper;

import org.geoserver.catalog.MapInfo;
import org.geoserver.jackson.databind.catalog.dto.Map;
import org.mapstruct.Mapper;

@Mapper(config = SpringCatalogInfoMapperConfig.class)
public interface MapMapper {

    MapInfo map(Map o);

    Map map(MapInfo o);
}
