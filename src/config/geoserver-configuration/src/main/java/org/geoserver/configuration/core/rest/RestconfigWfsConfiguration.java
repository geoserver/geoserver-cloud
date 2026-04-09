/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.core.rest;

import org.geoserver.config.GeoServer;
import org.geoserver.rest.service.WFSSettingsController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RestconfigWfsConfiguration {

    @Bean
    WFSSettingsController wfsSettingsController(GeoServer geoServer) {
        return new WFSSettingsController(geoServer);
    }
}
