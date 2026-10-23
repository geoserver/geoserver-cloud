/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.service;

import static org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheConfigurationProperties.SERVICE_WMS_ENABLED;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.configuration.gwc.GwcWMSServiceConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * @see GwcWMSServiceConfiguration
 * @since 1.0
 */
@AutoConfiguration
@ConditionalOnGeoWebCacheEnabled
@ConditionalOnProperty(name = SERVICE_WMS_ENABLED, havingValue = "true", matchIfMissing = false)
@ConditionalOnClass(name = "org.geowebcache.service.wms.WMSService")
@Import(org.geoserver.configuration.gwc.GwcWMSServiceConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.service")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class GwcWMSServiceAutoConfiguration {

    public @PostConstruct void log() {
        log.info("{} enabled", SERVICE_WMS_ENABLED);
    }
}
