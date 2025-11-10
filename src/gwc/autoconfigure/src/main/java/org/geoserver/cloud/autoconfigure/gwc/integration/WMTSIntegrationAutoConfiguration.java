/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.integration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnWMTSIntegrationEnabled;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geowebcache.service.wmts.WMTSService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * @since 1.0
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnWMTSIntegrationEnabled
@ConditionalOnClass(WMTSService.class)
@ImportFilteredResource("jar:gs-gwc-[0-9]+.*!/geowebcache-geoserver-wmts-integration.xml")
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.integration")
public class WMTSIntegrationAutoConfiguration {

    public @PostConstruct void log() {
        log.info("GeoWebCache WMTS GeoServer integration enabled");
    }
}
