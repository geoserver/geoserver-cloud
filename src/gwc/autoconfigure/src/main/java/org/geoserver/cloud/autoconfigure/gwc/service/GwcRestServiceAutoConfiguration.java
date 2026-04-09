/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheRestConfigEnabled;
import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheConfigurationProperties;
import org.geoserver.configuration.gwc.GwcRestConfiguration;
import org.geoserver.rest.RestControllerAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @see GwcRestConfiguration
 * @since 1.0
 */
@AutoConfiguration
@Import(GwcRestConfiguration.class)
@ConditionalOnGeoWebCacheRestConfigEnabled
@ConditionalOnClass(name = "org.geowebcache.rest.converter.GWCConverter")
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.service")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class GwcRestServiceAutoConfiguration {

    public @PostConstruct void log() {
        log.info("{} enabled", GeoWebCacheConfigurationProperties.RESTCONFIG_ENABLED);
    }

    /**
     * Since we don't scan the {@literal org.geoserver.rest}, we need a {@link RestControllerAdvice} explicitly to
     * handle http error code translations.
     *
     * <p>For example, it ensures that {@code org.geoserver.rest.ResourceNotFoundException} is correctly mapped to a 404
     * response instead of a default 500 error.
     */
    @Bean
    RestControllerAdvice restControllerAdvice() {
        return new RestControllerAdvice();
    }
}
