/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog;

import java.nio.file.Path;
import java.util.Properties;
import lombok.Data;
import org.geoserver.cloud.autoconfigure.catalog.JDBCConfigAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;

/**
 * Configuration properties to use GeoServer's {@code jdbcconfig} and {@code jdbcstore} community
 * modules as the {@link GeoServerBackendConfigurer catalog and configuration backend} through the
 * {@link JDBCConfigAutoConfiguration} auto-configuration.
 */
// @ConfigurationProperties(prefix = "geoserver.backend.jdbcconfig")
public @Data class JdbcconfigProperties {
    @Value("${geoserver.backend.jdbcconfig.enabled}")
    private boolean enabled;

    @Value("${geoserver.backend.jdbcconfig.initdb:false}")
    private boolean initdb;

    @Value("${geoserver.backend.jdbcconfig.cache-directory:}")
    private Path cacheDirectory;

    @Value("${geoserver.backend.jdbcconfig.datasource:}")
    private Properties datasource;
}
