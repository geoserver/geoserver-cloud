/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogEvent;
import org.geoserver.cloud.bus.event.config.RemoteConfigEvent;
import org.geoserver.config.GeoServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.bus.ConditionalOnBusEnabled;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Configuration to enable sending and receiving remote application events related to configuration
 * changes in the {@link Catalog} and {@link GeoServer}
 */
@Configuration
@ConditionalOnBusEnabled
@RemoteApplicationEventScan(
        basePackageClasses = {RemoteCatalogEvent.class, RemoteConfigEvent.class})
@ComponentScan(basePackageClasses = {RemoteCatalogEvent.class, RemoteConfigEvent.class})
@Slf4j(topic = "org.geoserver.cloud.bus")
public class RemoteApplicationEventsConfiguration {

    public RemoteApplicationEventsConfiguration() {}

    private @PostConstruct void logInit() {
        GeoServerBusProperties props = geoServerBusProperties();
        log.info(
                "Configuring GeoServer Catalog distributed events. Send-object: {}, send-diff: {}",
                props.isSendObject(),
                props.isSendDiff());
    }

    /** Determine the behavior of {@link RemoteEventPayloadCodec} */
    @ConfigurationProperties(prefix = "geoserver.bus")
    public @Bean GeoServerBusProperties geoServerBusProperties() {
        return new GeoServerBusProperties();
    }

    public @Bean GeoServerRemoteEventBroadcaster remoteEventBroadcaster() {
        return new GeoServerRemoteEventBroadcaster();
    }

    /**
     * @param rawCatalog used to evict cached live data sources from its {@link
     *     Catalog#getResourcePool() ResourcePool}
     */
    public @Bean RemoteEventResourcePoolProcessor remoteEventResourcePoolProcessor(
            @Qualifier("rawCatalog") Catalog rawCatalog) {
        return new RemoteEventResourcePoolProcessor(rawCatalog);
    }
}
