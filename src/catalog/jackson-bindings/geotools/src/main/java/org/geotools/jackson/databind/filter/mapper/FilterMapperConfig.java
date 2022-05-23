/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
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
        })
public class FilterMapperConfig {}
