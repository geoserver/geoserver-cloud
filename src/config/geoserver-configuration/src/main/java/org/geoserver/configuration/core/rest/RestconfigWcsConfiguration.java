/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.core.rest;

import org.geoserver.config.GeoServer;
import org.geoserver.rest.service.WCSSettingsController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RestconfigWcsConfiguration {

    @Bean
    WCSSettingsController wcsSettingsController(GeoServer geoServer) {
        return new WCSSettingsController(geoServer);
    }
}
