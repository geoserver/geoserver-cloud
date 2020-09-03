/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.security;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnGeoServerSecurityDisabled
@Slf4j
public class GeoServerSecurityDisabledAutoConfiguration {

    private @Value("${geoserver.security.enabled:#{null}}") Boolean enabled;

    public @PostConstruct void log() {
        log.info(
                "GeoServer security auto-configuration disabled explicitly through geoserver.security.enabled");
    }
}
