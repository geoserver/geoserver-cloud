/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.security;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.autoconfigure.catalog.backend.core.GeoServerBackendAutoConfiguration;
import org.geoserver.cloud.event.security.SecurityConfigEvent;
import org.geoserver.cloud.security.GeoServerSecurityConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;

@Configuration
@AutoConfigureAfter(GeoServerBackendAutoConfiguration.class)
@Import({
    GeoServerSecurityAutoConfiguration.Enabled.class,
    GeoServerSecurityAutoConfiguration.Disabled.class
})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.security")
public class GeoServerSecurityAutoConfiguration {

    @ConditionalOnGeoServerSecurityEnabled
    @Import(GeoServerSecurityConfiguration.class)
    public @Configuration class Enabled {

        private @Value("${geoserver.security.enabled:#{null}}") Boolean enabled;

        public @PostConstruct void log() {
            if (enabled == null)
                log.info(
                        """
                        GeoServer security auto-configuration enabled automatically
                        (no geoserver.security.enabled configuration property provided)
                        """);
            else if (enabled.booleanValue())
                log.info(
                        """
                        GeoServer security auto-configuration enabled
                        explicitly through geoserver.security.enabled: true
                        """);
        }
    }

    @Configuration
    @ConditionalOnGeoServerSecurityDisabled
    static class Disabled {

        private @Value("${geoserver.security.enabled:#{null}}") Boolean enabled;

        public @PostConstruct void log() {
            log.info(
                    """
                    GeoServer security auto-configuration disabled explicitly
                    with geoserver.security.enabled: false
                    """);
        }

        @EventListener(SecurityConfigEvent.class)
        public void onRemoteSecurityConfigChangeEvent(SecurityConfigEvent event) {
            if (!event.isLocal()) {
                log.info(
                        """
                        Security change event ignored, security is disabled on this service. {}
                        """,
                        event);
            }
        }
    }
}
