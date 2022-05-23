/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.bus;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.bus.ConditionalOnBusEnabled;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/** Log a message if spring-cloud-bus is explicitly disables */
@Configuration
@ConditionalOnProperty(
        value = ConditionalOnBusEnabled.SPRING_CLOUD_BUS_ENABLED,
        matchIfMissing = false,
        havingValue = "false")
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.bus")
public class RemoteApplicationEventsDisabledAutoConfiguration {

    public @PostConstruct void logBusDisabled() {
        log.warn("GeoServer Catalog and configuration event-bus is disabled");
    }
}
