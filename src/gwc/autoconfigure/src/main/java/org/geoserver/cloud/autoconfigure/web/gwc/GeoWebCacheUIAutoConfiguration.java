/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.gwc;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheRestConfigEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnWebUIEnabled;
import org.geoserver.cloud.gwc.config.core.GeoWebCacheConfigurationProperties;
import org.geowebcache.GeoWebCacheDispatcher;
import org.geowebcache.rest.controller.ByteStreamController;
import org.gwc.web.rest.GeoWebCacheController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnWebUIEnabled
// if this was in the extensions we should use @ConditionalOnGeoServerGWC
@ConditionalOnProperty(name = "geoserver.service.gwc.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.web.gwc")
public class GeoWebCacheUIAutoConfiguration {

    public @PostConstruct void log() {
        log.info("{} enabled", GeoWebCacheConfigurationProperties.WEBUI_ENABLED);
    }

    @Bean
    GeoWebCacheController gwcController(GeoWebCacheDispatcher gwcDispatcher) {
        return new GeoWebCacheController(gwcDispatcher);
    }

    /**
     * Provide a handler for static web resources if missing, for example, because {@link
     * ConditionalOnGeoWebCacheRestConfigEnabled} is disabled
     */
    @Bean
    @ConditionalOnMissingBean(ByteStreamController.class)
    ByteStreamController byteStreamController() {
        return new ByteStreamController();
    }
}
