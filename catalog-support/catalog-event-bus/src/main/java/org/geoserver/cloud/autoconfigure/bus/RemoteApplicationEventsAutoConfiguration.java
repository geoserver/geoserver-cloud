/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.bus;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.cloud.bus.RemoteApplicationEventsConfiguration;
import org.geoserver.cloud.bus.event.RemoteInfoEvent;
import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
import org.geoserver.jackson.databind.config.GeoServerConfigModule;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.ConditionalOnBusEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration auto-configuration} for {@link
 * RemoteApplicationEventsConfiguration}
 */
@Configuration
@ConditionalOnBusEnabled
@AutoConfigureAfter(BusAutoConfiguration.class)
@Import({RemoteApplicationEventsConfiguration.class})
public class RemoteApplicationEventsAutoConfiguration {

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
    public @Bean RemoteInfoEventInboundResolver remoteInfoEventInboundResolver() {
        return new RemoteInfoEventInboundResolver();
    }
}
