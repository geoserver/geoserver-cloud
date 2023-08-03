/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.event.bus;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;

/** Log a message if spring-cloud-bus is explicitly disables */
@AutoConfiguration
@Import({
    GeoServerBusIntegrationAutoConfiguration.Enabled.class,
    GeoServerBusIntegrationAutoConfiguration.Disabled.class
})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.bus")
public class GeoServerBusIntegrationAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnGeoServerRemoteEventsEnabled
    @AutoConfigureAfter(BusAutoConfiguration.class)
    static class Enabled {
        public @PostConstruct void logBusDisabled() {
            log.info("GeoServer event-bus integration is enabled");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnGeoServerRemoteEventsDisabled
    static class Disabled {
        public @PostConstruct void logBusDisabled() {
            log.warn("GeoServer event-bus integration is disabled by configuration");
        }
    }
}
