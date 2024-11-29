/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.event.bus;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.catalog.event.ConditionalOnCatalogEvents;
import org.geoserver.cloud.event.bus.RemoteGeoServerEventsConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.context.annotation.Import;

/** Log a message if spring-cloud-bus is explicitly disables */
@AutoConfiguration
@Import({GeoServerBusIntegrationAutoConfiguration.Enabled.class, GeoServerBusIntegrationAutoConfiguration.Disabled.class
})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.bus")
public class GeoServerBusIntegrationAutoConfiguration {

    @AutoConfiguration
    @AutoConfigureAfter(BusAutoConfiguration.class)
    @ConditionalOnCatalogEvents
    @ConditionalOnGeoServerRemoteEventsEnabled
    @Import(RemoteGeoServerEventsConfiguration.class)
    static class Enabled {
        public @PostConstruct void logBusDisabled() {
            log.info("GeoServer event-bus integration is enabled");
        }
    }

    @AutoConfiguration
    @ConditionalOnGeoServerRemoteEventsDisabled
    static class Disabled {
        public @PostConstruct void logBusDisabled() {
            log.warn("GeoServer event-bus integration is disabled by configuration");
        }
    }
}
