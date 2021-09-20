/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.security;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.config.security.GeoServerSecurityConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ConditionalOnGeoServerSecurityEnabled
@Import(GeoServerSecurityConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.security")
public @Configuration class GeoServerSecurityEnabledAutoConfiguration {

    private @Value("${geoserver.security.enabled:#{null}}") Boolean enabled;

    public @PostConstruct void log() {
        if (enabled == null)
            log.info(
                    "GeoServer security auto-configuration enabled automatically (no config property geoserver.security.enabled provided)");
        else if (enabled.booleanValue())
            log.info(
                    "GeoServer security auto-configuration enabled explicitly through config property");
    }
}
