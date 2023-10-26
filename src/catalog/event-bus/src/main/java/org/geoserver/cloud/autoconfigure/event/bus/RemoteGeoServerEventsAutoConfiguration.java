/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.event.bus;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.cloud.autoconfigure.catalog.event.ConditionalOnCatalogEvents;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.bus.InfoEventResolver;
import org.geoserver.cloud.event.bus.RemoteGeoServerEvent;
import org.geoserver.cloud.event.bus.RemoteGeoServerEventBridge;
import org.geoserver.cloud.event.bus.RemoteGeoServerEventMapper;
import org.geoserver.config.GeoServer;
import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
import org.geoserver.jackson.databind.config.GeoServerConfigModule;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.jackson.databind.filter.GeoToolsFilterModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * {@link EnableAutoConfiguration auto-configuration} catalog and config events integration with
 * spring cloud bus
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnCatalogEvents
@ConditionalOnGeoServerRemoteEventsEnabled
@AutoConfigureAfter(BusAutoConfiguration.class)
@RemoteApplicationEventScan(basePackageClasses = {RemoteGeoServerEvent.class})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.bus.catalog")
public class RemoteGeoServerEventsAutoConfiguration {

    /**
     * Add a {@link GeoToolsFilterModule} to the default jackson spring codecs if not already
     * present, so {@link Expression} and {@link Filter} objects can be used as {@link
     * RemoteGeoServerEvent} payload, especially {@link Literal} expressions.
     */
    @ConditionalOnMissingBean(GeoToolsFilterModule.class)
    public @Bean GeoToolsFilterModule geoToolsFilterModule() {
        return new GeoToolsFilterModule();
    }

    /**
     * Add a {@link GeoServerCatalogModule} to the default jackson spring codecs if not already
     * present, so {@link CatalogInfo} objects can be used as {@link RemoteGeoServerEvent} payload
     */
    @ConditionalOnMissingBean(GeoServerCatalogModule.class)
    public @Bean GeoServerCatalogModule geoServerCatalogJacksonModule() {
        return new GeoServerCatalogModule();
    }

    /**
     * Add a {@link GeoServerConfigModule} to the default jackson spring codecs if not already
     * present, so configuration {@link Info} objects can be used as {@link RemoteGeoServerEvent}
     * payload
     */
    @ConditionalOnMissingBean(GeoServerConfigModule.class)
    public @Bean GeoServerConfigModule geoServerConfigJacksonModule() {
        return new GeoServerConfigModule();
    }

    /**
     * Highest priority listener for incoming {@link RemoteGeoServerEvent} events to resolve the
     * payload {@link CatalogInfo} properties, as they may come either as {@link ResolvingProxy}
     * proxies, or {@code null} in case of collection properties.
     *
     * <p>This listener ensures the payload object properties are resolved before being catch up by
     * other listeners.
     */
    public @Bean InfoEventResolver remoteInfoEventInboundResolver(
            @Qualifier("rawCatalog") Catalog rawCatalog, GeoServer geoserver) {
        return new InfoEventResolver(rawCatalog, geoserver);
    }

    public @Bean RemoteGeoServerEventMapper remoteGeoServerEventMapper(
            InfoEventResolver remoteEventPropertiesResolver,
            ServiceMatcher serviceMatcher,
            Destination.Factory destinationFactory) {

        return new RemoteGeoServerEventMapper(
                remoteEventPropertiesResolver, serviceMatcher, destinationFactory);
    }

    public @Bean RemoteGeoServerEventBridge remoteEventBroadcaster(
            ApplicationEventPublisher eventPublisher,
            RemoteGeoServerEventMapper eventMapper,
            ServiceMatcher serviceMatcher) {

        log.info("Configuring GeoServer Catalog distributed events.");

        Consumer<GeoServerEvent<?>> localEventPublisher = eventPublisher::publishEvent;
        Consumer<RemoteApplicationEvent> remoteEventPublisher = eventPublisher::publishEvent;
        Supplier<String> busId = serviceMatcher::getBusId;
        return new RemoteGeoServerEventBridge(
                localEventPublisher, remoteEventPublisher, eventMapper, busId);
    }
}
