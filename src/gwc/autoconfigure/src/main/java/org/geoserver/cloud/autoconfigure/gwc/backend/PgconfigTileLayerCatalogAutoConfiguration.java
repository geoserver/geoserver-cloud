/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.backend;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.ConditionalOnPgconfigBackendEnabled;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigDataSourceAutoConfiguration;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.gwc.backend.pgconfig.CachingTileLayerInfoRepository;
import org.geoserver.cloud.gwc.backend.pgconfig.PgconfigTileLayerCatalog;
import org.geoserver.cloud.gwc.backend.pgconfig.PgconfigTileLayerInfoRepository;
import org.geoserver.cloud.gwc.backend.pgconfig.TileLayerInfoRepository;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.cloud.gwc.repository.GeoServerTileLayerConfiguration;
import org.geoserver.gwc.ConfigurableBlobStore;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.gwc.config.GWCInitializer;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geowebcache.grid.GridSetBroker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * {@link AutoConfiguration @AutoConfiguration} to set up the GeoServer {@link TileLayerCatalog}
 * using the "pgconfig" module implementation to store the tile layer configuration as part of the
 * GeoServer Catalog in the Postgres database;
 *
 * @since 1.7
 * @see ConditionalOnPgconfigBackendEnabled
 */
@AutoConfiguration(after = PgconfigDataSourceAutoConfiguration.class)
@ConditionalOnClass(PgconfigTileLayerCatalog.class)
@ConditionalOnGeoWebCacheEnabled
@ConditionalOnPgconfigBackendEnabled
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.backend")
public class PgconfigTileLayerCatalogAutoConfiguration {

    @PostConstruct
    void log() {
        log.info("GeoWebCache TileLayerCatalog using PostgreSQL config backend");
    }

    /** Replacement for {@link GWCInitializer} when using {@link GeoServerTileLayerConfiguration} */
    @Bean
    PgconfigGwcInitializer gwcInitializer(
            GWCConfigPersister configPersister,
            ConfigurableBlobStore blobStore,
            GeoServerTileLayerConfiguration tileLayerCatalog) {
        return new PgconfigGwcInitializer(configPersister, blobStore, tileLayerCatalog);
    }

    @Bean(name = "gwcCatalogConfiguration")
    GeoServerTileLayerConfiguration pgconfigTileLayerCatalog(
            GridSetBroker gridsetBroker,
            TileLayerInfoRepository repository,
            ApplicationEventPublisher eventPublisher,
            @Qualifier("rawCatalog") Catalog catalog) {

        var config = new PgconfigTileLayerCatalog(repository, gridsetBroker, () -> catalog);
        Consumer<TileLayerEvent> gwcEventPublisher = eventPublisher::publishEvent;
        return new GeoServerTileLayerConfiguration(config, gwcEventPublisher);
    }

    @Bean
    TileLayerInfoRepository pgconfigTileLayerRepository(
            @Qualifier("pgconfigDataSource") DataSource dataSource,
            Optional<CacheManager> cacheManager) {

        var pgrepo = new PgconfigTileLayerInfoRepository(new JdbcTemplate(dataSource));
        return cacheManager.map(cm -> cachingTileLayerCatalog(pgrepo, cm)).orElse(pgrepo);
    }

    TileLayerInfoRepository cachingTileLayerCatalog(
            TileLayerInfoRepository delegate, CacheManager cacheManager) {

        log.info(
                "GeoWebCache Caching TileLayerInfoRepository enabled. Repository: {}, CacheManager: {}",
                delegate.getClass().getName(),
                cacheManager.getClass().getName());

        Cache nameCache = getCache(cacheManager, CachingTileLayerInfoRepository.CACHE_NAME);
        return new CachingTileLayerInfoRepository(delegate, nameCache);
    }

    @NonNull
    private Cache getCache(CacheManager cacheManager, String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        Objects.requireNonNull(
                cache,
                """
                CacheManager %s returned null cache for cache name %s.
                Make sure the configuration includes spring.cache.cache-names: %s[,<...>]
                """
                        .formatted(cacheManager.getClass().getName(), cacheName, cacheName));
        return cache;
    }
}
