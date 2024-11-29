/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.jdbcconfig;

import java.nio.file.Path;
import lombok.Data;
import org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig.JDBCConfigAutoConfiguration;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to use GeoServer's {@code jdbcconfig} and {@code jdbcstore} community
 * modules as the {@link GeoServerBackendConfigurer catalog and configuration backend} through the
 * {@link JDBCConfigAutoConfiguration} auto-configuration.
 */
@Data
@ConfigurationProperties(prefix = "geoserver.backend.jdbcconfig")
public class JdbcConfigConfigurationProperties {
    private boolean enabled;
    private boolean initdb;
    private Path cacheDirectory;
    private DataSourceProperties datasource;
}
