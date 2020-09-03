/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog;

import java.nio.file.Path;
import java.util.Properties;
import lombok.Data;

public @Data class GeoServerBackendProperties {

    private DataDirectoryProperties dataDirectory = new DataDirectoryProperties();

    private JdbcconfigProperties jdbcconfig = new JdbcconfigProperties();

    public static @Data class DataDirectoryProperties {
        private boolean enabled;
        private Path location;
    }

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
}
