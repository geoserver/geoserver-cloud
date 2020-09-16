package org.geotools.jackson.databind.filter.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(config = FilterMapperConfig.class)
public class FilterMapper {

    public org.geotools.jackson.databind.filter.dto.Filter map(org.opengis.filter.Filter filter) {
        return Mappers.getMapper(FilterToDtoMapper.class).map(filter);
    }

    public org.opengis.filter.Filter map(org.geotools.jackson.databind.filter.dto.Filter dto) {
        return Mappers.getMapper(DtoToFilterMapper.class).map(dto);
    }
}
