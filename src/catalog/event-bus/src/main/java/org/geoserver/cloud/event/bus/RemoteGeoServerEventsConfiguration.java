/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.bus;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.config.GeoServer;
import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
import org.geoserver.jackson.databind.config.GeoServerConfigModule;
import org.geoserver.platform.config.UpdateSequence;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.jackson.databind.filter.GeoToolsFilterModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/** Catalog and config events integration with spring cloud bus */
@Configuration(proxyBeanMethods = false)
@RemoteApplicationEventScan(basePackageClasses = {RemoteGeoServerEvent.class})
@Slf4j(topic = "org.geoserver.cloud.event.bus")
public class RemoteGeoServerEventsConfiguration {

    /**
     * Add a {@link GeoToolsFilterModule} to the default jackson spring codecs if not already
     * present, so {@link Expression} and {@link Filter} objects can be used as {@link
     * RemoteGeoServerEvent} payload, especially {@link Literal} expressions.
     */
    @Bean
    @ConditionalOnMissingBean(GeoToolsFilterModule.class)
    GeoToolsFilterModule geoToolsFilterModule() {
        return new GeoToolsFilterModule();
    }

    /**
     * Add a {@link GeoServerCatalogModule} to the default jackson spring codecs if not already
     * present, so {@link CatalogInfo} objects can be used as {@link RemoteGeoServerEvent} payload
     */
    @Bean
    @ConditionalOnMissingBean(GeoServerCatalogModule.class)
    GeoServerCatalogModule geoServerCatalogJacksonModule() {
        return new GeoServerCatalogModule();
    }

    /**
     * Add a {@link GeoServerConfigModule} to the default jackson spring codecs if not already
     * present, so configuration {@link Info} objects can be used as {@link RemoteGeoServerEvent}
     * payload
     */
    @Bean
    @ConditionalOnMissingBean(GeoServerConfigModule.class)
    GeoServerConfigModule geoServerConfigJacksonModule() {
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
    @Bean
    InfoEventResolver remoteInfoEventInboundResolver(
            @Qualifier("rawCatalog") Catalog rawCatalog, GeoServer geoserver) {
        return new InfoEventResolver(rawCatalog, geoserver);
    }

    @Bean
    RemoteGeoServerEventMapper remoteGeoServerEventMapper(
            InfoEventResolver remoteEventPropertiesResolver,
            ServiceMatcher serviceMatcher,
            Destination.Factory destinationFactory) {

        return new RemoteGeoServerEventMapper(
                remoteEventPropertiesResolver, serviceMatcher, destinationFactory);
    }

    @Bean
    RemoteGeoServerEventBridge remoteEventBroadcaster(
            ApplicationEventPublisher eventPublisher,
            RemoteGeoServerEventMapper eventMapper,
            UpdateSequence updateSequence) {

        log.info("Configuring GeoServer Catalog distributed events.");

        Consumer<GeoServerEvent> localEventPublisher = eventPublisher::publishEvent;
        Consumer<RemoteGeoServerEvent> remoteEventPublisher = eventPublisher::publishEvent;
        return new RemoteGeoServerEventBridge(
                localEventPublisher, remoteEventPublisher, eventMapper, updateSequence);
    }
}
