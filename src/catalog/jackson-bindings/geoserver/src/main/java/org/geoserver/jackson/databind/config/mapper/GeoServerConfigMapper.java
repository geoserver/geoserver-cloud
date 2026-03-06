/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.config.mapper;

import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.cog.CogSettings;
import org.geoserver.cog.CogSettingsStore;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInfo.WebUIMode;
import org.geoserver.config.ImageProcessingInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.UserDetailsDisplaySettingsInfo;
import org.geoserver.config.UserDetailsDisplaySettingsInfo.EmailDisplayMode;
import org.geoserver.config.UserDetailsDisplaySettingsInfo.LoggedInUserDisplayMode;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.gwc.wmts.WMTSInfoImpl;
import org.geoserver.jackson.databind.catalog.dto.CatalogInfoDto;
import org.geoserver.jackson.databind.catalog.dto.InfoDto;
import org.geoserver.jackson.databind.catalog.mapper.CatalogInfoMapper;
import org.geoserver.jackson.databind.config.dto.CogSettingsDto;
import org.geoserver.jackson.databind.config.dto.CogSettingsStoreDto;
import org.geoserver.jackson.databind.config.dto.ConfigInfoDto;
import org.geoserver.jackson.databind.config.dto.ContactInfoDto;
import org.geoserver.jackson.databind.config.dto.CoverageAccessInfoDto;
import org.geoserver.jackson.databind.config.dto.GeoServerInfoDto;
import org.geoserver.jackson.databind.config.dto.GeoServerInfoDto.ResourceErrorHandlingDto;
import org.geoserver.jackson.databind.config.dto.GeoServerInfoDto.UserDetailsDisplaySettingsInfoDto.EmailDisplayModeDto;
import org.geoserver.jackson.databind.config.dto.GeoServerInfoDto.UserDetailsDisplaySettingsInfoDto.LoggedInUserDisplayModeDto;
import org.geoserver.jackson.databind.config.dto.GeoServerInfoDto.WebUIModeDto;
import org.geoserver.jackson.databind.config.dto.ImageProcessingInfoDto;
import org.geoserver.jackson.databind.config.dto.LoggingInfoDto;
import org.geoserver.jackson.databind.config.dto.ServiceInfoDto;
import org.geoserver.jackson.databind.config.dto.SettingsInfoDto;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.WCSInfoImpl;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSInfoImpl;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.WPSInfoImpl;
import org.geotools.util.Version;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/** Mapper to/from GeoServer config objects and their respective DTO representations */
@Mapper(config = ConfigInfoMapperConfig.class)
@AnnotateWith(value = Generated.class)
public interface GeoServerConfigMapper {

    CatalogInfoMapper catalogInfoMapper = Mappers.getMapper(CatalogInfoMapper.class);

    default <T extends Info> T toInfo(InfoDto dto) {
        if (dto == null) {
            return null;
        } else if (dto instanceof ConfigInfoDto configInfo) {
            return toInfo(configInfo);
        } else if (dto instanceof CatalogInfoDto catalogInfo) {
            return catalogInfoMapper.map(catalogInfo);
        }
        throw new IllegalArgumentException(
                "Unknown config DTO type: " + dto.getClass().getCanonicalName());
    }

    @SuppressWarnings("unchecked")
    default <T extends InfoDto> T toDto(Info info) {
        if (info == null) {
            return null;
        } else if (info instanceof GeoServerInfo gs) {
            return (T) toDto(gs);
        } else if (info instanceof SettingsInfo settings) {
            return (T) toDto(settings);
        } else if (info instanceof LoggingInfo logging) {
            return (T) toDto(logging);
        } else if (info instanceof ServiceInfo service) {
            return (T) toDto(service);
        } else if (info instanceof CatalogInfo catInfo) {
            return (T) catalogInfoMapper.map(catInfo);
        }

        throw new IllegalArgumentException(
                "Unknown config info type: " + info.getClass().getCanonicalName());
    }

    @SuppressWarnings("unchecked")
    default <T extends Info> T toInfo(ConfigInfoDto dto) {
        if (dto == null) {
            return null;
        } else if (dto instanceof GeoServerInfoDto gs) {
            return (T) toInfo(gs);
        } else if (dto instanceof SettingsInfoDto settings) {
            return (T) toInfo(settings);
        } else if (dto instanceof LoggingInfoDto logging) {
            return (T) toInfo(logging);
        } else if (dto instanceof ServiceInfoDto service) {
            return (T) toInfo(service);
        }

        throw new IllegalArgumentException(
                "Unknown config DTO type: " + dto.getClass().getCanonicalName());
    }

    @Mapping(target = "id", ignore = true) // set by factory method
    @Mapping(target = "clientProperties", ignore = true)
    @Mapping(target = "useHeadersProxyURL", ignore = true) // deprecated
    @Mapping(target = "xmlExternalEntitiesEnabled", ignore = true) // deprecated
    @Mapping(
            target = "userDetailsDisplaySettings",
            defaultExpression = "java(new org.geoserver.config.impl.UserDetailsDisplaySettingsInfoImpl())")
    GeoServerInfo toInfo(GeoServerInfoDto dto);

    @Mapping(
            target = "userDetailsDisplaySettings",
            defaultExpression =
                    "java(new org.geoserver.jackson.databind.config.dto.GeoServerInfoDto.UserDetailsDisplaySettingsInfoDto())")
    GeoServerInfoDto toDto(GeoServerInfo info);

    UserDetailsDisplaySettingsInfo toInfo(GeoServerInfoDto.UserDetailsDisplaySettingsInfoDto dto);

    GeoServerInfoDto.UserDetailsDisplaySettingsInfoDto toDto(UserDetailsDisplaySettingsInfo info);

    ResourceErrorHandlingDto toDto(ResourceErrorHandling r);

    ResourceErrorHandling toInfo(ResourceErrorHandlingDto dto);

    WebUIModeDto toDto(WebUIMode m);

    WebUIMode toInfo(WebUIModeDto dto);

    @Mapping(target = "id", ignore = true) // set by factory method
    @Mapping(target = "clientProperties", ignore = true)
    SettingsInfo toInfo(SettingsInfoDto dto);

    SettingsInfoDto toDto(SettingsInfo info);

    LoggedInUserDisplayModeDto toDto(LoggedInUserDisplayMode mode);

    LoggedInUserDisplayMode toInfo(LoggedInUserDisplayModeDto dto);

    EmailDisplayModeDto toDto(EmailDisplayMode mode);

    EmailDisplayMode toInfo(EmailDisplayModeDto dto);

    @Mapping(target = "tileCache", ignore = true)
    @Mapping(target = "imageProcessing", ignore = true)
    ImageProcessingInfo imageProcessingInfo(ImageProcessingInfoDto dto);

    ImageProcessingInfoDto imageProcessingInfo(ImageProcessingInfo info);

    @Mapping(target = "id", ignore = true) // set by factory method
    LoggingInfo toInfo(LoggingInfoDto dto);

    LoggingInfoDto toDto(LoggingInfo info);

    @Mapping(target = "threadPoolExecutor", ignore = true)
    CoverageAccessInfo coverageAccessInfo(CoverageAccessInfoDto dto);

    CoverageAccessInfoDto coverageAccessInfo(CoverageAccessInfo info);

    @Mapping(target = "id", ignore = true) // set by factory method
    ContactInfo contactInfo(ContactInfoDto dto);

    ContactInfoDto contactInfo(ContactInfo info);

    default ServiceInfo toInfo(ServiceInfoDto dto) {
        if (dto == null) {
            return null;
        } else if (dto instanceof ServiceInfoDto.WmsService wms) {
            return toInfo(wms);
        } else if (dto instanceof ServiceInfoDto.WfsService wfs) {
            return toInfo(wfs);
        } else if (dto instanceof ServiceInfoDto.WcsService wcs) {
            return toInfo(wcs);
        } else if (dto instanceof ServiceInfoDto.WpsService wps) {
            return toInfo(wps);
        } else if (dto instanceof ServiceInfoDto.WmtsService wmts) {
            return toInfo(wmts);
        } else if (dto instanceof ServiceInfoDto.GenericService s) {
            return toInfo(s);
        }

        throw new IllegalArgumentException(
                "Unknown ServiceInfo type: " + dto.getClass().getCanonicalName());
    }

    default ServiceInfoDto toDto(ServiceInfo info) {
        if (info == null) {
            return null;
        } else if (info instanceof WMSInfo wms) {
            return toDto(wms);
        } else if (info instanceof WFSInfo wfs) {
            return toDto(wfs);
        } else if (info instanceof WCSInfo wcs) {
            return toDto(wcs);
        } else if (info instanceof WPSInfo wps) {
            return toDto(wps);
        } else if (info instanceof WMTSInfo wmts) {
            return toDto(wmts);
        } else if (info.getClass().equals(ServiceInfoImpl.class)) {
            return toGenericService(info);
        }

        throw new IllegalArgumentException(
                "Unknown ServiceInfo type: " + info.getClass().getCanonicalName());
    }

    /**
     * {@link ServiceInfo#getVersions()} does not parameterize the list, hence Mapstruct assigns the
     * {@code List<String>} as is
     */
    default List<org.geotools.util.Version> stringListToVersionList(List<String> list) {
        return list == null ? null : list.stream().map(Version::new).collect(toCollection(ArrayList::new));
    }

    @Mapping(target = "clientProperties", ignore = true)
    @Mapping(target = "geoServer", ignore = true)
    @Mapping(target = "versions", expression = "java(stringListToVersionList(dto.getVersions()))")
    WMSInfoImpl toInfo(ServiceInfoDto.WmsService dto);

    ServiceInfoDto.WmsService toDto(WMSInfo info);

    @Mapping(target = "clientProperties", ignore = true)
    @Mapping(target = "geoServer", ignore = true)
    @Mapping(target = "versions", expression = "java(stringListToVersionList(dto.getVersions()))")
    WFSInfoImpl toInfo(ServiceInfoDto.WfsService dto);

    ServiceInfoDto.WfsService toDto(WFSInfo info);

    @Mapping(target = "clientProperties", ignore = true)
    @Mapping(target = "geoServer", ignore = true)
    @Mapping(target = "versions", expression = "java(stringListToVersionList(dto.getVersions()))")
    WCSInfoImpl toInfo(ServiceInfoDto.WcsService dto);

    ServiceInfoDto.WcsService toDto(WCSInfo info);

    @Mapping(target = "clientProperties", ignore = true)
    @Mapping(target = "geoServer", ignore = true)
    @Mapping(target = "versions", expression = "java(stringListToVersionList(dto.getVersions()))")
    WPSInfoImpl toInfo(ServiceInfoDto.WpsService dto);

    ServiceInfoDto.WpsService toDto(WPSInfo info);

    @Mapping(target = "clientProperties", ignore = true)
    @Mapping(target = "geoServer", ignore = true)
    @Mapping(target = "versions", expression = "java(stringListToVersionList(dto.getVersions()))")
    WMTSInfoImpl toInfo(ServiceInfoDto.WmtsService dto);

    @Mapping(target = "clientProperties", ignore = true)
    @Mapping(target = "geoServer", ignore = true)
    @Mapping(target = "versions", expression = "java(stringListToVersionList(dto.getVersions()))")
    ServiceInfoImpl toInfo(ServiceInfoDto.GenericService dto);

    ServiceInfoDto.GenericService toGenericService(ServiceInfo info);

    ServiceInfoDto.WmtsService toDto(WMTSInfo info);

    CogSettings cogSettings(CogSettingsDto dto);

    CogSettingsDto cogSettings(CogSettings info);

    CogSettingsStore cogSettingsStore(CogSettingsStoreDto dto);

    CogSettingsStoreDto cogSettingsStore(CogSettingsStore info);
}
