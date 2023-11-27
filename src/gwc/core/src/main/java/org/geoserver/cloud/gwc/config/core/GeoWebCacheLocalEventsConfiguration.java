/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import org.geoserver.cloud.gwc.event.TileLayerEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class GeoWebCacheLocalEventsConfiguration {

    @Bean
    TileLayerEventPublisher tileLayerEventPublisher() {
        return new TileLayerEventPublisher();
    }
}
