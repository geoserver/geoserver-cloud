/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto.mapper;

import org.geoserver.jackson.databind.catalog.mapper.ValueMappers;
import org.geoserver.jackson.databind.mapper.SharedMappers;
import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
    componentModel = "default",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    uses = {SharedMappers.class, ObjectFacotries.class, WPSMapper.class, ValueMappers.class}
)
public class ConfigInfoMapperConfig {}
