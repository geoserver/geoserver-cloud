/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.mapper;

import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.filter.SortByImpl;
import org.geotools.jackson.databind.filter.dto.Expression;
import org.geotools.jackson.databind.filter.dto.SortBy;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(config = FilterMapperConfig.class)
public class FilterMapper {

    public org.geotools.jackson.databind.filter.dto.Filter map(
            org.geotools.api.filter.Filter filter) {
        return Mappers.getMapper(FilterToDtoMapper.class).map(filter);
    }

    public org.geotools.api.filter.Filter map(org.geotools.jackson.databind.filter.dto.Filter dto) {
        return Mappers.getMapper(DtoToFilterMapper.class).map(dto);
    }

    public org.geotools.api.filter.sort.SortBy map(
            org.geotools.jackson.databind.filter.dto.SortBy dto) {
        if (dto == null) return null;
        PropertyName propertyName =
                Mappers.getMapper(ExpressionMapper.class).map(dto.getPropertyName());

        SortOrder sortOrder = map(dto.getSortOrder());
        return new SortByImpl(propertyName, sortOrder);
    }

    public org.geotools.jackson.databind.filter.dto.SortBy map(
            org.geotools.api.filter.sort.SortBy sortBy) {
        if (sortBy == null) return null;
        Expression.PropertyName propertyName =
                Mappers.getMapper(ExpressionMapper.class).map(sortBy.getPropertyName());
        org.geotools.jackson.databind.filter.dto.SortBy.SortOrder sortOrder =
                map(sortBy.getSortOrder());
        return new SortBy(propertyName, sortOrder);
    }

    public SortOrder map(org.geotools.jackson.databind.filter.dto.SortBy.SortOrder order) {
        return order == null
                        || order
                                == org.geotools.jackson.databind.filter.dto.SortBy.SortOrder
                                        .ASCENDING
                ? SortOrder.ASCENDING
                : SortOrder.DESCENDING;
    }

    public org.geotools.jackson.databind.filter.dto.SortBy.SortOrder map(SortOrder order) {
        return order == null || SortOrder.ASCENDING == order
                ? SortBy.SortOrder.ASCENDING
                : SortBy.SortOrder.DESCENDING;
    }
}
