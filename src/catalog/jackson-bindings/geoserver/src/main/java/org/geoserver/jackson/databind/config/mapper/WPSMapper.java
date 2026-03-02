/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.config.mapper;

import lombok.Generated;
import org.geoserver.jackson.databind.catalog.mapper.GeoServerValueObjectsMapper;
import org.geoserver.jackson.databind.config.dto.ServiceInfoDto;
import org.geoserver.jackson.databind.config.dto.ServiceInfoDto.WpsService.ProcessGroup;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessGroupInfoImpl;
import org.geoserver.wps.ProcessInfo;
import org.geoserver.wps.ProcessInfoImpl;
import org.geotools.jackson.databind.filter.mapper.GeoToolsValueMappers;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import org.mapstruct.ReportingPolicy;

@Mapper(
        uses = {GeoServerValueObjectsMapper.class, GeoToolsValueMappers.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Generated.class)
public interface WPSMapper {

    default @ObjectFactory ProcessGroupInfo processGroupInfo() {
        return new ProcessGroupInfoImpl();
    }

    default @ObjectFactory ProcessInfo processInfo() {
        return new ProcessInfoImpl();
    }

    ProcessInfo map(ServiceInfoDto.WpsService.Process p);

    ServiceInfoDto.WpsService.Process map(ProcessInfo p);

    ProcessGroup map(ProcessGroupInfo info);

    ProcessGroupInfo map(ProcessGroup dto);
}
