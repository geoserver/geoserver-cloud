/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.jdbcconfig;

import lombok.Data;
import lombok.Generated;

import org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig.JDBCConfigAutoConfiguration;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Configuration properties to use GeoServer's {@code jdbcconfig} and {@code jdbcstore} community
 * modules as the {@link GeoServerBackendConfigurer catalog and configuration backend} through the
 * {@link JDBCConfigAutoConfiguration} auto-configuration.
 */
@Generated
@ConfigurationProperties(prefix = "geoserver.backend.jdbcconfig")
public @Data class JdbcConfigConfigurationProperties {
    private boolean enabled;
    private boolean initdb;
    private Path cacheDirectory;
    private DataSourceProperties datasource;
}
