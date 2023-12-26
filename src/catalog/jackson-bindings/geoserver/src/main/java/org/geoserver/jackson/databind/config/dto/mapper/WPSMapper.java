/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config.dto.mapper;

import lombok.Generated;

import org.geoserver.jackson.databind.catalog.mapper.ValueMappers;
import org.geoserver.jackson.databind.config.dto.Service;
import org.geoserver.jackson.databind.mapper.SharedMappers;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessGroupInfoImpl;
import org.geoserver.wps.ProcessInfo;
import org.geoserver.wps.ProcessInfoImpl;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import org.mapstruct.ReportingPolicy;

@Mapper(
        uses = {ValueMappers.class, SharedMappers.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Generated.class)
public interface WPSMapper {

    default @ObjectFactory ProcessGroupInfo processGroupInfo() {
        return new ProcessGroupInfoImpl();
    }

    default @ObjectFactory ProcessInfo processInfo() {
        return new ProcessInfoImpl();
    }

    ProcessInfo map(Service.WpsService.Process p);

    Service.WpsService.Process map(ProcessInfo p);
}
