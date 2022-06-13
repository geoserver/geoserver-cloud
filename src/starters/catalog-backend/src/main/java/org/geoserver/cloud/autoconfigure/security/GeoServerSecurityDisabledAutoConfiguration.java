/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.security;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.event.security.SecurityConfigEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnGeoServerSecurityDisabled
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.security")
public class GeoServerSecurityDisabledAutoConfiguration {

    private @Value("${geoserver.security.enabled:#{null}}") Boolean enabled;

    public @PostConstruct void log() {
        log.info(
                "GeoServer security auto-configuration disabled explicitly through geoserver.security.enabled");
    }

    @EventListener(SecurityConfigEvent.class)
    public void onRemoteSecurityConfigChangeEvent(SecurityConfigEvent event) {
        if (!event.isLocal()) {
            log.info(
                    "Security change event ignored, security is disabled on this service. {}",
                    event);
        }
    }
}
