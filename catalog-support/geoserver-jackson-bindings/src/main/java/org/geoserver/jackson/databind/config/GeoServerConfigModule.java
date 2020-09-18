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
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
import org.geoserver.jackson.databind.config.dto.ConfigInfoDto;
import org.geoserver.jackson.databind.config.dto.GeoServer;
import org.geoserver.jackson.databind.config.dto.Logging;
import org.geoserver.jackson.databind.config.dto.Service;
import org.geoserver.jackson.databind.config.dto.Settings;

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
@Slf4j
public class GeoServerConfigModule extends SimpleModule {
    private static final long serialVersionUID = -8756800180255446679L;

    public GeoServerConfigModule() {
        super(GeoServerConfigModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

        log.debug("registering jackson de/serializers for all GeoServer config Info types");

        addSerializer(GeoServerInfo.class);
        addSerializer(SettingsInfo.class);
        addSerializer(LoggingInfo.class);
        addSerializer(ServiceInfo.class);

        addDeserializer(GeoServerInfo.class, GeoServer.class);
        addDeserializer(SettingsInfo.class, Settings.class);
        addDeserializer(LoggingInfo.class, Logging.class);
        addDeserializer(ServiceInfo.class, Service.class);
    }

    private <I extends Info> void addSerializer(Class<I> configInfoType) {
        log.trace("registering serializer for {}", configInfoType.getSimpleName());
        super.addSerializer(configInfoType, serializer(configInfoType));
    }

    private <I extends Info, D extends ConfigInfoDto> void addDeserializer(
            Class<I> infoType, Class<D> dtoType) {
        log.trace("registering deserializer for {}", infoType.getSimpleName());
        super.addDeserializer(infoType, deserializer(dtoType));
    }

    private <I extends Info> ConfigInfoSerializer<I> serializer(Class<I> configInfoType) {
        return new ConfigInfoSerializer<>(configInfoType);
    }

    private <I extends Info, D extends ConfigInfoDto> ConfigInfoDeserializer<I, D> deserializer(
            Class<D> dtoType) {
        return new ConfigInfoDeserializer<>(dtoType);
    }
}
