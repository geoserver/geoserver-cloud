/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jdbcconfig;

import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.jdbcconfig.web.JDBCConfigStatusProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JDBCConfigWebConfiguration {

    /**
     * Configure the web-ui component only if the web-ui is on the classpath AND the {@code
     * geoserver.backend.jdbcconfig.web.enabled} is true, defaulting to true
     */
    @Bean("JDBCConfigStatusProvider")
    public JDBCConfigStatusProvider jdbcConfigStatusProvider(JDBCConfigProperties config) {
        return new JDBCConfigStatusProvider(config);
    }
}
