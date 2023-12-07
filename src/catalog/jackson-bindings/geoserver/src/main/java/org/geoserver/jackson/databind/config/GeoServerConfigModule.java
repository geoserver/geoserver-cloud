/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Info;
import org.geoserver.cog.CogSettings;
import org.geoserver.cog.CogSettingsStore;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
import org.geoserver.jackson.databind.catalog.dto.InfoDto;
import org.geoserver.jackson.databind.config.dto.CogSettingsDto;
import org.geoserver.jackson.databind.config.dto.CogSettingsStoreDto;
import org.geoserver.jackson.databind.config.dto.Contact;
import org.geoserver.jackson.databind.config.dto.CoverageAccess;
import org.geoserver.jackson.databind.config.dto.GeoServer;
import org.geoserver.jackson.databind.config.dto.JaiDto;
import org.geoserver.jackson.databind.config.dto.Logging;
import org.geoserver.jackson.databind.config.dto.Service;
import org.geoserver.jackson.databind.config.dto.Settings;
import org.geoserver.jackson.databind.config.dto.mapper.GeoServerConfigMapper;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wps.WPSInfo;
import org.geotools.jackson.databind.util.MapperDeserializer;
import org.geotools.jackson.databind.util.MapperSerializer;
import org.mapstruct.factory.Mappers;

import java.util.function.Function;

/**
 * Jackson {@link com.fasterxml.jackson.databind.Module} to handle GeoServer configuration objects
 * ({@link GeoServerInfo} and related) bindings.
 *
 * <p>Depends on {@link GeoServerCatalogModule}
 *
 * <p>To register the module for a specific {@link ObjectMapper}, either:
 *
 * <pre>
 * <code>
 * ObjectMapper objectMapper = ...
 * objectMapper.findAndRegisterModules();
 * </code>
 * </pre>
 *
 * Or:
 *
 * <pre>
 * <code>
 * ObjectMapper objectMapper = ...
 * objectMapper.registerModule(new GeoServerConfigModule());
 * objectMapper.registerModule(new GeoServerCatalogModule());
 * </code>
 * </pre>
 */
@Slf4j(topic = "org.geoserver.jackson.databind.config")
public class GeoServerConfigModule extends SimpleModule {
    private static final long serialVersionUID = -8756800180255446679L;
    static final GeoServerConfigMapper VALUE_MAPPER =
            Mappers.getMapper(GeoServerConfigMapper.class);

    public GeoServerConfigModule() {
        super(GeoServerConfigModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

        log.debug("registering jackson de/serializers for all GeoServer config Info types");

        addSerializer(GeoServerInfo.class);
        addDeserializer(GeoServerInfo.class, GeoServer.class);

        addSerializer(SettingsInfo.class);
        addDeserializer(SettingsInfo.class, Settings.class);

        addSerializer(LoggingInfo.class);
        addDeserializer(LoggingInfo.class, Logging.class);

        addDeserializer(ServiceInfo.class, Service.class);
        addSerializer(ServiceInfo.class);

        addSerializer(WMSInfo.class);
        addDeserializer(WMSInfo.class, Service.WmsService.class);

        addSerializer(WFSInfo.class);
        addDeserializer(WFSInfo.class, Service.WfsService.class);

        addSerializer(WCSInfo.class);
        addDeserializer(WCSInfo.class, Service.WcsService.class);

        addSerializer(WPSInfo.class);
        addDeserializer(WPSInfo.class, Service.WpsService.class);

        addSerializer(WMTSInfo.class);
        addDeserializer(WMTSInfo.class, Service.WmtsService.class);

        registerValueSerializers();
    }

    protected void registerValueSerializers() {
        addMapperSerializer(
                CoverageAccessInfo.class,
                VALUE_MAPPER::coverageAccessInfo,
                CoverageAccess.class,
                VALUE_MAPPER::coverageAccessInfo);

        addMapperSerializer(
                JAIInfo.class, VALUE_MAPPER::jaiInfo, JaiDto.class, VALUE_MAPPER::jaiInfo);

        addMapperSerializer(
                ContactInfo.class,
                VALUE_MAPPER::contactInfo,
                Contact.class,
                VALUE_MAPPER::contactInfo);

        addMapperSerializer(
                CogSettings.class,
                VALUE_MAPPER::cogSettings,
                CogSettingsDto.class,
                VALUE_MAPPER::cogSettings);
        addMapperSerializer(
                CogSettingsStore.class,
                VALUE_MAPPER::cogSettingsStore,
                CogSettingsStoreDto.class,
                VALUE_MAPPER::cogSettingsStore);
    }

    /**
     * @param <T> object model type
     * @param <D> DTO type
     */
    private <T, D> void addMapperSerializer(
            Class<T> type,
            Function<T, D> serializerMapper,
            Class<D> dtoType,
            Function<D, T> deserializerMapper) {

        MapperSerializer<T, D> serializer = new MapperSerializer<>(type, serializerMapper);
        MapperDeserializer<D, T> deserializer =
                new MapperDeserializer<>(dtoType, deserializerMapper);
        super.addSerializer(type, serializer);
        super.addDeserializer(type, deserializer);
    }

    private <I extends Info> void addSerializer(Class<I> configInfoType) {
        log.trace("registering serializer for {}", configInfoType.getSimpleName());
        super.addSerializer(configInfoType, serializer(configInfoType));
    }

    private <I extends Info, D extends InfoDto> void addDeserializer(
            Class<I> infoType, Class<D> dtoType) {
        log.trace("registering deserializer for {}", infoType.getSimpleName());
        super.addDeserializer(infoType, deserializer(dtoType));
    }

    private <I extends Info> ConfigInfoSerializer<I> serializer(Class<I> configInfoType) {
        return new ConfigInfoSerializer<>(configInfoType);
    }

    private <I extends Info, D extends InfoDto> ConfigInfoDeserializer<I, D> deserializer(
            Class<D> dtoType) {
        return new ConfigInfoDeserializer<>(dtoType);
    }
}
