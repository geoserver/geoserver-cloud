/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog;

import java.nio.file.Path;
import java.util.Properties;
import lombok.Data;
import org.geoserver.cloud.autoconfigure.catalog.CatalogServiceBackendAutoConfiguration;
import org.geoserver.cloud.autoconfigure.catalog.DataDirectoryAutoConfiguration;
import org.geoserver.cloud.autoconfigure.catalog.JDBCConfigAutoConfiguration;

public @Data class GeoServerBackendProperties {

    private DataDirectoryProperties dataDirectory = new DataDirectoryProperties();

    private JdbcconfigProperties jdbcconfig = new JdbcconfigProperties();

    private CatalogService catalogService = new CatalogService();

    /**
     * Configuration properties to use GeoServer's traditiona, file-system based data-directory as
     * the {@link GeoServerBackendConfigurer catalog and configuration backend} through the {@link
     * DataDirectoryAutoConfiguration} auto-configuration.
     */
    public static @Data class DataDirectoryProperties {
        private boolean enabled;
        private Path location;
    }

    /**
     * Configuration properties to use GeoServer's {@code jdbcconfig} and {@code jdbcstore}
     * community modules as the {@link GeoServerBackendConfigurer catalog and configuration backend}
     * through the {@link JDBCConfigAutoConfiguration} auto-configuration.
     */
    public static @Data class JdbcconfigProperties {
        private boolean enabled;
        private boolean initdb;
        private Web web = new Web();
        private Path cacheDirectory;
        private Properties datasource;

        public static @Data class Web {
            private boolean enabled;
        }
    }

    /**
     * Configuration properties to use the {@code catalog-service} microservice as the {@link
     * GeoServerBackendConfigurer catalog and configuration backend} through the {@link
     * CatalogServiceBackendAutoConfiguration}
     */
    public static @Data class CatalogService {
        private boolean enabled;
        private String url;
    }
}
