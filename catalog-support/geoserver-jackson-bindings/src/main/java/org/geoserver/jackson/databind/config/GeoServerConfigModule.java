/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
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
public class GeoServerConfigModule extends SimpleModule {
    private static final long serialVersionUID = -8756800180255446679L;

    public GeoServerConfigModule() {
        super(GeoServerConfigModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

        addSerializer(new ConfigInfoSerializer<>(GeoServerInfo.class));
        addSerializer(new ConfigInfoSerializer<>(SettingsInfo.class));
        addSerializer(new ConfigInfoSerializer<>(LoggingInfo.class));
        addSerializer(new ConfigInfoSerializer<>(ServiceInfo.class));

        addDeserializer(
                GeoServerInfo.class,
                new ConfigInfoDeserializer<GeoServerInfo, GeoServer>(GeoServer.class));
        addDeserializer(
                SettingsInfo.class,
                new ConfigInfoDeserializer<SettingsInfo, Settings>(Settings.class));
        addDeserializer(
                LoggingInfo.class, new ConfigInfoDeserializer<LoggingInfo, Logging>(Logging.class));
        addDeserializer(
                ServiceInfo.class, new ConfigInfoDeserializer<ServiceInfo, Service>(Service.class));
    }
}
