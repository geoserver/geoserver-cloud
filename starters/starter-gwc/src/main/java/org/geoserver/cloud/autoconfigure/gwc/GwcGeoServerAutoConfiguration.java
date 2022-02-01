/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc;

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
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;

/** @since 1.0 */
@Configuration(proxyBeanMethods = true)
@AutoConfigureAfter(GwcCoreAutoConfiguration.class)
@ImportResource(
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = {
        "jar:gs-gwc-.*!/geowebcache-geoserver-context.xml#name=^(?!GeoSeverTileLayerCatalog|gwcCatalogConfiguration).*$"
    }
)
public class GwcGeoServerAutoConfiguration {

    @Bean(name = "gwcCatalogConfiguration")
    CatalogConfiguration gwcCatalogConfiguration( //
            @Qualifier("rawCatalog") Catalog catalog, //
            @Qualifier("GeoSeverTileLayerCatalog") TileLayerCatalog tld, //
            GridSetBroker gsb) {

        return new CloudCatalogConfiguration(catalog, tld, gsb);
    }

    @Primary
    @Bean(name = "GeoSeverTileLayerCatalog")
    public TileLayerCatalog cachingTileLayerCatalog(ResourceStoreTileLayerCatalog delegate) {
        CacheManager cacheManager = new CaffeineCacheManager();
        return new CachingTileLayerCatalog(cacheManager, delegate);
    }

    public @Bean ResourceStoreTileLayerCatalog resourceStoreTileLayerCatalog(
            @Qualifier("resourceStoreImpl") ResourceStore resourceStore) {
        return new ResourceStoreTileLayerCatalog(resourceStore);
    }
}
