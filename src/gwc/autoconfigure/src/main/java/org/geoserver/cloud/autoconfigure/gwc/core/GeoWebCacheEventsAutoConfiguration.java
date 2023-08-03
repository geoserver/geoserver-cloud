/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.core;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.gwc.config.core.GeoWebCacheLocalEventsConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;

/**
 * @see GeoWebCacheLocalEventsConfiguration
 * @see ConditionalOnGeoWebCacheEnabled
 * @since 1.0
 */
@AutoConfiguration
@ConditionalOnGeoWebCacheEnabled
@Import(GeoWebCacheLocalEventsConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.core")
public class GeoWebCacheEventsAutoConfiguration {

    public @PostConstruct void log() {
        log.info("GeoWebCache local events integration enabled");
    }
}
