/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.catalog.backend.core.GeoServerBackendAutoConfiguration;
import org.geoserver.cloud.event.security.SecurityConfigChanged;
import org.geoserver.cloud.security.GeoServerSecurityConfiguration;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

@AutoConfiguration(after = GeoServerBackendAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@Import({GeoServerSecurityAutoConfiguration.WhenEnabled.class, GeoServerSecurityAutoConfiguration.WhenDisabled.class})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.security")
public class GeoServerSecurityAutoConfiguration {

    @ConditionalOnGeoServerSecurityEnabled
    @Import(GeoServerSecurityConfiguration.class)
    @Configuration
    static class WhenEnabled {

        private @Value("${geoserver.security.enabled:#{null}}") Boolean enabled;

        public @PostConstruct void log() {
            if (enabled == null) {
                log.info(
                        """
                        GeoServer security auto-configuration enabled automatically \
                        (no geoserver.security.enabled configuration property provided)
                        """);
            } else if (enabled.booleanValue()) {
                log.info(
                        """
                        GeoServer security auto-configuration enabled \
                        explicitly through geoserver.security.enabled: true
                        """);
            }
        }

        /**
         * @since 1.3, required since geoserver 2.23.2
         */
        @Bean
        @ConditionalOnMissingBean
        XStreamPersisterFactory xstreamPersisterFactory() {
            return new XStreamPersisterFactory();
        }
    }

    @Configuration
    @ConditionalOnGeoServerSecurityDisabled
    static class WhenDisabled {

        private @Value("${geoserver.security.enabled:#{null}}") Boolean enabled;

        public @PostConstruct void log() {
            log.info(
                    """
                    GeoServer security auto-configuration disabled explicitly
                    with geoserver.security.enabled: false
                    """);
        }

        @EventListener(SecurityConfigChanged.class)
        public void onRemoteSecurityConfigChangeEvent(SecurityConfigChanged event) {
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
