/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog;

import org.geoserver.cloud.autoconfigure.EnableBusEventHandling;
import org.geoserver.cloud.autoconfigure.EnableJdbcConfig;
import org.geoserver.cloud.config.main.GeoServerSecurityConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(GeoServerSecurityConfiguration.class)
@EnableAutoConfiguration
@EnableJdbcConfig
@EnableBusEventHandling
public class GeoServerCatalogConfig {
}
