/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.gwc.repository.CachingTileLayerCatalog;
import org.geoserver.cloud.gwc.repository.CloudCatalogConfiguration;
import org.geoserver.cloud.gwc.repository.ResourceStoreTileLayerCatalog;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.resource.ResourceStore;
import org.geowebcache.grid.GridSetBroker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import javax.annotation.PostConstruct;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = true)
@ImportResource(
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = {
            "jar:gs-gwc-[0-9]+.*!/geowebcache-geoserver-context.xml#name=^(?!"
                    + GeoServerIntegrationConfiguration.EXCLUDED_BEANS
                    + ").*$"
        })
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.core")
public class GeoServerIntegrationConfiguration {

    static final String EXCLUDED_BEANS = //
            """
		GeoSeverTileLayerCatalog\
		|gwcCatalogConfiguration\
		|gwcTransactionListener\
		|gwcWMSExtendedCapabilitiesProvider""";

    public @PostConstruct void log() {
        log.info("GeoWebCache core GeoServer integration enabled");
    }

    @Bean(name = "gwcCatalogConfiguration")
    CatalogConfiguration gwcCatalogConfiguration( //
            @Qualifier("rawCatalog") Catalog catalog, //
            @Qualifier("GeoSeverTileLayerCatalog") TileLayerCatalog tld, //
            GridSetBroker gsb) {

        return new CloudCatalogConfiguration(catalog, tld, gsb);
    }

    @Primary
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
