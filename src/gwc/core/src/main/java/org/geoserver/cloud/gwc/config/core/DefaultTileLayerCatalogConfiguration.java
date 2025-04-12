/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.core;

import java.util.Optional;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.gwc.repository.CachingTileLayerCatalog;
import org.geoserver.cloud.gwc.repository.CloudCatalogConfiguration;
import org.geoserver.cloud.gwc.repository.GeoServerTileLayerConfiguration;
import org.geoserver.cloud.gwc.repository.ResourceStoreTileLayerCatalog;
import org.geoserver.gwc.ConfigurableBlobStore;
import org.geoserver.gwc.config.DefaultGwcInitializer;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.gwc.config.GWCInitializer;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.resource.ResourceStore;
import org.geowebcache.config.TileLayerConfiguration;
import org.geowebcache.grid.GridSetBroker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.context.WebApplicationContext;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class DefaultTileLayerCatalogConfiguration {

    /**
     * Replaces {@link GWCInitializer}
     *
     * <p>
     *
     * <ul>
     *   <li>We don't need to upgrade from very old configuration settings
     *   <li>{@code GWCInitializer} depends on {@link TileLayerCatalog}, assuming {@link
     *       CatalogConfiguration} is the only tile layer storage backend for geoserver tile layers,
     *       and it's not the case for GS cloud
     */
    @Bean
    DefaultGwcInitializer gwcInitializer(
            GWCConfigPersister configPersister,
            ConfigurableBlobStore blobStore,
            GeoServerTileLayerConfiguration geoseverTileLayers,
            GeoServerConfigurationLock lock) {
        return new DefaultGwcInitializer(configPersister, blobStore, geoseverTileLayers, lock);
    }

    /**
     * In vanilla GeoServer, {@link CatalogConfiguration} is the {@link TileLayerConfiguration}
     * contributed to the app context to serve {@code TileLayer}s ({@link GeoServerTileLayer}) out
     * of the GeoServer {@link Catalog} by means of a {@link TileLayerCatalog}.
     *
     * <p>Here we contribute a different {@code TileLayerConfiguration} for the same purpose, {@link
     * GeoServerTileLayerConfiguration}, which is a distributed-event aware decorator over the
     * actual {@link CloudCatalogConfiguration} implementation of {@code TileLayerCatalog}.
     *
     * <p>Since the {@code CloudCatalogConfiguration} isn't hence a spring bean, in order to avoid
     * registering as a delegate to {@link TileLayerDispatcher}, {@link TileLayerEvents} will need
     * to be relayed from {@code GeoServerTileLayerConfiguration} to {@link
     * CloudCatalogConfiguration#onTileLayerEventEvict()}.
     */
    @SuppressWarnings("java:S6830")
    @Bean(name = "gwcCatalogConfiguration")
    GeoServerTileLayerConfiguration gwcCatalogConfiguration( //
            @Qualifier("rawCatalog") Catalog catalog, //
            @Qualifier("GeoSeverTileLayerCatalog") TileLayerCatalog tld, //
            GridSetBroker gsb,
            ApplicationEventPublisher eventPublisher) {

        var config = new CloudCatalogConfiguration(catalog, tld, gsb);
        var eventAwareConfig = new GeoServerTileLayerConfiguration(config, eventPublisher::publishEvent);
        // tell GeoServerTileLayerConfiguration to relay TileLayerEvents to
        // CloudCatalogConfiguration, since it's not a spring bean can't listen itself.
        eventAwareConfig.setEventListener(config::onTileLayerEventEvict);
        return eventAwareConfig;
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
            @Qualifier("resourceStoreImpl") ResourceStore resourceStore, Optional<WebApplicationContext> webappCtx) {
        return new ResourceStoreTileLayerCatalog(resourceStore, webappCtx);
    }
}
