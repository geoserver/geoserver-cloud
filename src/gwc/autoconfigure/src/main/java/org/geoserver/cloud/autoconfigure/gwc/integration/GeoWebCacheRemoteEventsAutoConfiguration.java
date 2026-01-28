/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.integration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.event.bus.ConditionalOnGeoServerRemoteEventsEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.gwc.config.bus.GeoWebCacheRemoteEventsConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @see GeoWebCacheRemoteEventsConfiguration
 * @see ConditionalOnGeoWebCacheEnabled
 * @see ConditionalOnGeoServerRemoteEventsEnabled
 * @since 1.0
 */
@AutoConfiguration(after = BusAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnGeoWebCacheEnabled
@ConditionalOnGeoServerRemoteEventsEnabled
@Import(GeoWebCacheRemoteEventsConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.integration")
public class GeoWebCacheRemoteEventsAutoConfiguration {

    public @PostConstruct void log() {
        log.info("GeoWebCache remote events integration enabled");
    }
}
