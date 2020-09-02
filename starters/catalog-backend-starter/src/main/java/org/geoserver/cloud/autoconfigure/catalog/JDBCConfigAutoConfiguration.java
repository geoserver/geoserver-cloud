/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.config.jdbcconfig.JDBCConfigBackendConfigurer;
import org.geoserver.cloud.config.jdbcconfig.JDBCDataSourceConfiguration;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcconfig.config.JDBCGeoServerFacade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({JDBCCatalogFacade.class, JDBCGeoServerFacade.class})
@ConditionalOnJdbcConfigEnabled
@Import({JDBCConfigBackendConfigurer.class, JDBCDataSourceConfiguration.class})
@Slf4j
public class JDBCConfigAutoConfiguration {

    public @PostConstruct void log() {
        log.info("Processing geoserver config backend with {}", getClass().getSimpleName());
    }
}
