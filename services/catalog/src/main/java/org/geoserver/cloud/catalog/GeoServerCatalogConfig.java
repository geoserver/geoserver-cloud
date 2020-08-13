/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.catalog;

import org.geoserver.cloud.autoconfigure.EnableBusEventHandling;
import org.geoserver.cloud.autoconfigure.EnableJdbcConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@EnableJdbcConfig
@EnableBusEventHandling
public class GeoServerCatalogConfig {}
