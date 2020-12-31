/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog;

import org.geoserver.cloud.bus.incoming.caching.RemoteEventCacheEvictor;
import org.geoserver.cloud.catalog.caching.CachingCatalogFacade;
import org.geoserver.cloud.catalog.caching.CachingGeoServerFacade;
import org.geoserver.cloud.catalog.caching.GeoServerBackendCacheConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration @EnableAutoConfiguration} auto configuration for geoserver's
 * catalog back-end caching using spring {@link CacheManager}.
 *
 * <p>Caching for the geoserver backend is enabled conditionally on property {@code
 * geoserver.catalog.caching.enabled=true}, defaults to {@code false}.
 *
 * @see GeoServerBackendCacheConfiguration
 */
@Configuration
@ConditionalOnBackendCacheEnabled
@Import(GeoServerBackendCacheConfiguration.class)
public class BackendCacheAutoConfiguration {

    private @Autowired CachingCatalogFacade cachingCatalogFacade;
    private @Autowired CachingGeoServerFacade cachingGeoServerFacade;

    public @Bean RemoteEventCacheEvictor remoteEventCacheEvictor() {
        return new RemoteEventCacheEvictor(cachingCatalogFacade, cachingGeoServerFacade);
    }
}
