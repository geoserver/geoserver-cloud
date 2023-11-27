/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.cache;

import org.geoserver.cloud.autoconfigure.catalog.event.ConditionalOnCatalogEvents;
import org.geoserver.cloud.autoconfigure.catalog.event.LocalCatalogEventsAutoConfiguration;
import org.geoserver.cloud.catalog.cache.CachingCatalogFacade;
import org.geoserver.cloud.catalog.cache.CachingGeoServerFacade;
import org.geoserver.cloud.catalog.cache.GeoServerBackendCacheConfiguration;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.remote.cache.RemoteEventCacheEvictor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration @EnableAutoConfiguration} auto configuration for evicting catalog
 * backend cache entries upon remote {@link InfoEvent}s.
 *
 * @see GeoServerBackendCacheConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(LocalCatalogEventsAutoConfiguration.class)
@ConditionalOnBackendCacheEnabled
@ConditionalOnCatalogEvents
public class RemoteEventCacheAutoConfiguration {

    @Bean
    RemoteEventCacheEvictor remoteEventCacheEvictor(
            CachingCatalogFacade cachingCatalogFacade,
            CachingGeoServerFacade cachingGeoServerFacade) {

        return new RemoteEventCacheEvictor(cachingCatalogFacade, cachingGeoServerFacade);
    }
}
