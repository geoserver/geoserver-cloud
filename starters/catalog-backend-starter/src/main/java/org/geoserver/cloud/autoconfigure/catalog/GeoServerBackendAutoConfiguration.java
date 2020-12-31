/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@Configuration
@EnableConfigurationProperties
@Import({
    DataDirectoryAutoConfiguration.class,
    JDBCConfigAutoConfiguration.class,
    JDBCConfigWebAutoConfiguration.class,
    CatalogClientBackendAutoConfiguration.class,
    BackendCacheAutoConfiguration.class
})
public class GeoServerBackendAutoConfiguration {}
