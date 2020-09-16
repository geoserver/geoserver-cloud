package org.geoserver.cloud.catalog.modelmapper;

import org.geoserver.catalog.MapInfo;
import org.geoserver.cloud.catalog.dto.Map;
import org.mapstruct.Mapper;

@Mapper(config = SpringCatalogInfoMapperConfig.class)
public interface MapMapper {

    MapInfo map(Map o);

    Map map(MapInfo o);
}
