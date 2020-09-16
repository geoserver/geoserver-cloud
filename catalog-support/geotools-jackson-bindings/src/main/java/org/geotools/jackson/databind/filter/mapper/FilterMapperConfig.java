package org.geotools.jackson.databind.filter.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
    componentModel = "default",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    uses = {
        ExpressionFactory.class,
        FilterFactory.class,
        ValueMappers.class,
        ExpressionMapper.class
    }
)
public class FilterMapperConfig {}
