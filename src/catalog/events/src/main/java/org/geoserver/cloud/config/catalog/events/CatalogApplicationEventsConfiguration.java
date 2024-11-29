/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.events;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.lifecycle.LifecycleEvent;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.config.UpdateSequence;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CatalogApplicationEventsConfiguration {

    @Bean
    CatalogApplicationEventPublisher localApplicationEventPublisher( //
            @Qualifier("catalog") Catalog catalog, //
            @Qualifier("geoServer") GeoServer geoServer, //
            ApplicationEventPublisher localContextPublisher, //
            UpdateSequence updateSequence //
            ) {

        Consumer<? super InfoEvent> publisher = localContextPublisher::publishEvent;
        Supplier<Long> updateSequenceIncrementor = updateSequence::nextValue;
        return new CatalogApplicationEventPublisher(publisher, catalog, geoServer, updateSequenceIncrementor);
    }

    @Bean
    GeoServerLifecycleEventPublisher localGeoServerLifecycleEventPublisher(
            ApplicationEventPublisher localContextPublisher) {
        Consumer<? super LifecycleEvent> publisher = localContextPublisher::publishEvent;

        return new GeoServerLifecycleEventPublisher(publisher);
    }
}
