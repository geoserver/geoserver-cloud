/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog;

import org.geoserver.cloud.config.catalog.CoreBackendConfiguration;
import org.geotools.autoconfigure.httpclient.GeoToolsHttpClientAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Groups imports to all the available backend auto-configurations
 *
 * <p>Interested auto-configuration classes can use a single reference to this class in {@link
 * AutoConfigureAfter @AutoConfigureAfter} without having to know what specific kind of backend
 * configurations there exist, and also we only need to register this single class in {@code
 * META-INF/spring.factories} regardless of how many backend configs there are in the future
 * (provided their configs are included in this class' {@code @Import} list)
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(GeoToolsHttpClientAutoConfiguration.class)
@Import({ //
    CoreBackendConfiguration.class, //
    DataDirectoryAutoConfiguration.class, //
    JDBCConfigAutoConfiguration.class, //
    JDBCConfigWebAutoConfiguration.class, //
    CatalogClientBackendAutoConfiguration.class, //
    BackendCacheAutoConfiguration.class //
})
public class GeoServerBackendAutoConfiguration {

    // // REVISIT: @ConfigurationProperties is not working
    // // @ConfigurationProperties(prefix = "geoserver.backend")
    // public @Bean GeoServerBackendProperties geoServerBackendProperties() {
    // GeoServerBackendProperties props = new GeoServerBackendProperties();
    // props.setCatalogService(catalogServiceClientProperties());
    // props.setDataDirectory(dataDirectoryProperties());
    // props.setJdbcconfig(jdbcconfigProperties());
    // return props;
    // }
    //
    // // REVISIT: @ConfigurationProperties is not working
    // // @ConfigurationProperties(prefix = "geoserver.backend.data-directory")
    // public @Bean DataDirectoryProperties dataDirectoryProperties() {
    // return new DataDirectoryProperties();
    // }
    //
    // // REVISIT: @ConfigurationProperties is not working
    // // @ConfigurationProperties(prefix = "geoserver.backend.jdbcconfig")
    // public @Bean JdbcconfigProperties jdbcconfigProperties() {
    // return new JdbcconfigProperties();
    // }
    //
    // // REVISIT: @ConfigurationProperties is not working
    // // @ConfigurationProperties(prefix = "geoserver.backend.catalog-service")
    // public @Bean CatalogClientProperties catalogServiceClientProperties() {
    // return new CatalogClientProperties();
    // }
}
