/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.integration.local;

import org.geowebcache.GeoWebCacheException;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.StorageException;
import org.gwc.tiling.service.CacheJobManagerImpl;
import org.gwc.tiling.service.CacheJobRegistry;
import org.gwc.tiling.service.CacheJobRequestBuilder;
import org.gwc.tiling.service.TileCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class GeoWebCacheJobsConfiguration {

    @Bean
    public CacheJobManagerImpl localCacheJobManager( //
            CacheJobRegistry registry, //
            Supplier<CacheJobRequestBuilder> requestBuilderFactory) {

        return new CacheJobManagerImpl(registry, requestBuilderFactory);
    }

    @Bean
    CacheJobRegistry cacheJobRegistry() {
        return new CacheJobRegistry();
    }

    @Bean
    TileCacheManager tileCacheManager(TileLayerDispatcher tld) {
        return new TileCacheManager(new DefaultTileLayerSeederResolver(tld));
    }

    @Bean
    Supplier<CacheJobRequestBuilder> cacheJobRequestBuilderFactory(
            TileLayerDispatcher tld, StorageBroker sb) {
        Function<String, TileLayer> tileLayerResolver =
                t -> {
                    try {
                        return tld.getTileLayer(t);
                    } catch (GeoWebCacheException e) {
                        throw new IllegalStateException(e);
                    }
                };

        Function<String, Set<String>> paramsIdsResolver =
                t -> {
                    try {
                        return sb.getCachedParameterIds(t);
                    } catch (StorageException e) {
                        throw new IllegalStateException(e);
                    }
                };

        return () ->
                new CacheJobRequestBuilder(
                        tileLayerResolver.andThen(TileLayerInfoAdapter::new), paramsIdsResolver);
    }
}
