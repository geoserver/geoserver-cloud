/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.service;

import static org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheConfigurationProperties.SERVICE_TMS_ENABLED;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.configuration.gwc.GwcTMSServiceConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Honors {@link ConditionalOnGeoWebCacheEnabled}: the service dispatches to the GeoWebCache core, which only the roles
 * hosting it run. The class check alone cannot tell them apart on a union classpath.
 *
 * @see GwcTMSServiceConfiguration
 * @since 1.0
 */
@AutoConfiguration
@ConditionalOnGeoWebCacheEnabled
@ConditionalOnProperty(name = SERVICE_TMS_ENABLED, havingValue = "true", matchIfMissing = false)
@ConditionalOnClass(name = "org.geowebcache.service.tms.TMSService")
@Import(org.geoserver.configuration.gwc.GwcTMSServiceConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.service")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class GwcTMSServiceAutoConfiguration {

    public @PostConstruct void log() {
        log.info("{} enabled", SERVICE_TMS_ENABLED);
    }
}
