/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.jdbcconfig;

import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.jdbcconfig.web.JDBCConfigStatusProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** Configuration for the wicket ui components of the jdbcconfig extension */
@ConditionalOnClass(name = "org.geoserver.web.GeoServerHomePageContentProvider")
@ConditionalOnProperty(prefix = "geoserver.jdbcconfig.web", name = "enabled", matchIfMissing = true)
@Import({JDBCDataSourceConfiguration.class})
public class JDBCConfigWebConfiguration {

    /**
     * Configure the web-ui component only if the web-ui is on the classpath AND the {@code
     * geoserver.jdbcconfig.web.enabled} is true, defaulting to true
     */
    @Bean("JDBCConfigStatusProvider")
    public JDBCConfigStatusProvider jdbcConfigStatusProvider(JDBCConfigProperties config) {
        return new JDBCConfigStatusProvider(config);
    }
}
