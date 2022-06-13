/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.events;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.config.GeoServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration(proxyBeanMethods = false)
public class CatalogApplicationEventsConfiguration {

    public @Bean CatalogApplicationEventPublisher localApplicationEventPublisher( //
            ApplicationEventPublisher localContextPublisher, //
            @Qualifier("catalog") Catalog catalog, //
            @Qualifier("geoServer") GeoServer geoServer //
            ) {

        Consumer<? super InfoEvent<?, ?, ?>> publisher = localContextPublisher::publishEvent;
        return new CatalogApplicationEventPublisher(publisher, catalog, geoServer);
    }
}
