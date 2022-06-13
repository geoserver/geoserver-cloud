/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.bus.catalog;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.cloud.autoconfigure.bus.ConditionalOnGeoServerRemoteEventsEnabled;
import org.geoserver.cloud.autoconfigure.catalog.event.ConditionalOnCatalogEvents;
import org.geoserver.cloud.bus.catalog.InfoEventResolver;
import org.geoserver.cloud.bus.catalog.RemoteCatalogEventBridge;
import org.geoserver.cloud.bus.catalog.RemoteCatalogEventMapper;
import org.geoserver.cloud.bus.catalog.RemoteInfoEvent;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.config.GeoServer;
import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
import org.geoserver.jackson.databind.config.GeoServerConfigModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * {@link EnableAutoConfiguration auto-configuration} catalog and config events integration with
 * spring cloud bus
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnCatalogEvents
@ConditionalOnGeoServerRemoteEventsEnabled
@AutoConfigureAfter(BusAutoConfiguration.class)
@RemoteApplicationEventScan(basePackageClasses = {RemoteInfoEvent.class})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.bus.catalog")
public class RemoteCatalogEventsAutoConfiguration {

    /**
     * Add a {@link GeoServerCatalogModule} to the default jackson spring codecs if not already
     * present, so {@link CatalogInfo} objects can be used as {@link RemoteInfoEvent} payload
     */
    @ConditionalOnMissingBean(GeoServerCatalogModule.class)
    public @Bean GeoServerCatalogModule geoServerCatalogJacksonModule() {
        return new GeoServerCatalogModule();
    }

    /**
     * Add a {@link GeoServerConfigModule} to the default jackson spring codecs if not already
     * present, so configuration {@link Info} objects can be used as {@link RemoteInfoEvent} payload
     */
    @ConditionalOnMissingBean(GeoServerConfigModule.class)
    public @Bean GeoServerConfigModule geoServerConfigJacksonModule() {
        return new GeoServerConfigModule();
    }

    /**
     * Highest priority listener for incoming {@link RemoteInfoEvent} events to resolve the payload
     * {@link CatalogInfo} properties, as they may come either as {@link ResolvingProxy} proxies, or
     * {@code null} in case of collection properties.
     *
     * <p>This listener ensures the payload object properties are resolved before being catch up by
     * other listeners.
     */
    public @Bean InfoEventResolver remoteInfoEventInboundResolver(
            @Qualifier("rawCatalog") Catalog rawCatalog, GeoServer geoserver) {
        return new InfoEventResolver(rawCatalog, geoserver);
    }

    public @Bean RemoteCatalogEventMapper remoteGeoServerEventMapper(
            InfoEventResolver remoteEventPropertiesResolver,
            ServiceMatcher serviceMatcher,
            Destination.Factory destinationFactory) {

        return new RemoteCatalogEventMapper(
                remoteEventPropertiesResolver, serviceMatcher, destinationFactory);
    }

    public @Bean RemoteCatalogEventBridge remoteEventBroadcaster(
            ApplicationEventPublisher eventPublisher, RemoteCatalogEventMapper eventMapper) {

        log.info("Configuring GeoServer Catalog distributed events.");

        Consumer<RemoteInfoEvent> remoteEventPublisher = eventPublisher::publishEvent;
        Consumer<InfoEvent<?, ?, ?>> localEventPublisher = eventPublisher::publishEvent;
        return new RemoteCatalogEventBridge(remoteEventPublisher, localEventPublisher, eventMapper);
    }
}
