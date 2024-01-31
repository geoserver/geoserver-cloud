/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.cloud.gwc.repository.CachingTileLayerCatalog;
import org.geoserver.cloud.gwc.repository.CloudCatalogConfiguration;
import org.geoserver.cloud.gwc.repository.GeoServerTileLayerConfiguration;
import org.geoserver.cloud.gwc.repository.ResourceStoreTileLayerCatalog;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.resource.ResourceStore;
import org.geowebcache.grid.GridSetBroker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class DefaultTileLayerCatalogConfiguration {

    @SuppressWarnings("java:S6830")
    @Bean(name = "gwcCatalogConfiguration")
    GeoServerTileLayerConfiguration gwcCatalogConfiguration( //
            @Qualifier("rawCatalog") Catalog catalog, //
            @Qualifier("GeoSeverTileLayerCatalog") TileLayerCatalog tld, //
            GridSetBroker gsb,
            ApplicationEventPublisher eventPublisher) {

        var config = new CloudCatalogConfiguration(catalog, tld, gsb);
        Consumer<TileLayerEvent> gwcEventPublisher = eventPublisher::publishEvent;
        return new GeoServerTileLayerConfiguration(config, gwcEventPublisher);
    }

    @Primary
    @SuppressWarnings("java:S6830")
    @Bean(name = "GeoSeverTileLayerCatalog")
    TileLayerCatalog cachingTileLayerCatalog(ResourceStoreTileLayerCatalog delegate) {
        CacheManager cacheManager = new CaffeineCacheManager();
        return new CachingTileLayerCatalog(cacheManager, delegate);
    }

    @Bean
    ResourceStoreTileLayerCatalog resourceStoreTileLayerCatalog(
            @Qualifier("resourceStoreImpl") ResourceStore resourceStore,
            Optional<WebApplicationContext> webappCtx) {
        return new ResourceStoreTileLayerCatalog(resourceStore, webappCtx);
    }
}
