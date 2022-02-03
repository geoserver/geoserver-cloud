/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.integration;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.gwc.event.TileLayerEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** @since 1.0 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnGeoWebCacheEnabled
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.integration")
public class LocalEventsAutoConfiguration {

    public @PostConstruct void log() {
        log.info("GeoWebCache local events integration enabled");
    }

    public @Bean TileLayerEventPublisher tileLayerEventPublisher() {
        return new TileLayerEventPublisher();
    }
}
