/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.gwc;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheRestConfigEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnWebUIEnabled;
import org.geoserver.cloud.gwc.config.core.GeoWebCacheConfigurationProperties;
import org.geowebcache.rest.controller.ByteStreamController;
import org.gwc.web.rest.GeoWebCacheController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;

@AutoConfiguration
@ConditionalOnWebUIEnabled
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.web.gwc")
public class GeoWebCacheUIAutoConfiguration {

    public @PostConstruct void log() {
        log.info("{} enabled", GeoWebCacheConfigurationProperties.WEBUI_ENABLED);
    }

    @Bean
    GeoWebCacheController gwcController() {
        return new GeoWebCacheController();
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
