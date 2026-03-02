/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.mapper;

import lombok.Generated;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.filter.SortByImpl;
import org.geotools.jackson.databind.filter.dto.ExpressionDto;
import org.geotools.jackson.databind.filter.dto.SortByDto;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(config = FilterMapperConfig.class)
@AnnotateWith(value = Generated.class)
public class FilterMapper {

    public org.geotools.jackson.databind.filter.dto.FilterDto map(org.geotools.api.filter.Filter filter) {
        return Mappers.getMapper(FilterToDtoMapper.class).map(filter);
    }

    public org.geotools.api.filter.Filter map(org.geotools.jackson.databind.filter.dto.FilterDto dto) {
        return Mappers.getMapper(DtoToFilterMapper.class).map(dto);
    }

    public SortBy dtoToSortBy(SortByDto dto) {
        if (dto == null) {
            return null;
        }
        PropertyName propertyName = Mappers.getMapper(ExpressionMapper.class).map(dto.getPropertyName());

        SortOrder sortOrder = map(dto.getSortOrder());
        return new SortByImpl(propertyName, sortOrder);
    }

    public SortByDto sortByToDto(SortBy sortBy) {
        if (sortBy == null) {
            return null;
        }
        ExpressionDto.PropertyNameDto propertyName =
                Mappers.getMapper(ExpressionMapper.class).map(sortBy.getPropertyName());
        org.geotools.jackson.databind.filter.dto.SortByDto.SortOrderDto sortOrder = map(sortBy.getSortOrder());
        return new SortByDto(propertyName, sortOrder);
    }

    public SortOrder map(org.geotools.jackson.databind.filter.dto.SortByDto.SortOrderDto order) {
        return order == null || order == org.geotools.jackson.databind.filter.dto.SortByDto.SortOrderDto.ASCENDING
                ? SortOrder.ASCENDING
                : SortOrder.DESCENDING;
    }

    public org.geotools.jackson.databind.filter.dto.SortByDto.SortOrderDto map(SortOrder order) {
        return order == null || SortOrder.ASCENDING == order
                ? SortByDto.SortOrderDto.ASCENDING
                : SortByDto.SortOrderDto.DESCENDING;
    }
}
