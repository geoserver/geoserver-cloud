/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.backend;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.requestcache.RemoteChangeCounter;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.ConditionalOnPgconfigBackendEnabled;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigDataSourceAutoConfiguration;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheMetrics;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheProperties;
import org.geoserver.cloud.backend.pgconfig.cache.PutBudget;
import org.geoserver.cloud.config.catalog.backend.pgconfig.ConditionalOnPgconfigCacheEnabled;
import org.geoserver.cloud.gwc.backend.pgconfig.BuiltTileLayerCache;
import org.geoserver.cloud.gwc.backend.pgconfig.PgconfigGwcCatalogRenameListener;
import org.geoserver.cloud.gwc.backend.pgconfig.PgconfigTileLayerCatalog;
import org.geoserver.cloud.gwc.backend.pgconfig.PgconfigTileLayerEventListener;
import org.geoserver.cloud.gwc.backend.pgconfig.PgconfigTileLayerInfoRepository;
import org.geoserver.cloud.gwc.backend.pgconfig.RequestCachingTileLayerInfoRepository;
import org.geoserver.cloud.gwc.backend.pgconfig.TileLayerInfoRepository;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.cloud.gwc.repository.GeoServerTileLayerConfiguration;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.gwc.config.GWCInitializer;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geowebcache.grid.GridSetBroker;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@link AutoConfiguration @AutoConfiguration} to set up the GeoServer {@link TileLayerCatalog} using the "pgconfig"
 * module implementation to store the tile layer configuration as part of the GeoServer Catalog in the Postgres
 * database.
 *
 * @since 1.7
 * @see ConditionalOnPgconfigBackendEnabled
 */
@AutoConfiguration(after = PgconfigDataSourceAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnClass(PgconfigTileLayerCatalog.class)
@ConditionalOnGeoWebCacheEnabled
@ConditionalOnPgconfigBackendEnabled
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.backend")
public class PgconfigTileLayerCatalogAutoConfiguration {

    @PostConstruct
    void log() {
        log.info("GeoWebCache TileLayerCatalog using PostgreSQL config backend");
    }

    /**
     * Replacement for {@link GWCInitializer} when using {@link GeoServerTileLayerConfiguration}
     *
     * @param configLock
     */
    @Bean
    PgconfigGwcInitializer gwcInitializer(GWCConfigPersister configPersister, GeoServerConfigurationLock configLock) {
        return new PgconfigGwcInitializer(configPersister, configLock);
    }

    /**
     * Propagates Workspace / Resource / LayerGroup name changes to the GeoWebCache storage broker so the file blob
     * store directory and pgconfig disk-quota tileset rows follow. Pgconfig derives tile-layer names from
     * {@code PublishedInfo} on every lookup, which breaks upstream {@code CatalogLayerEventListener}'s rename path
     * (lookup by old prefixed name returns empty after the SQL trigger refreshes the materialized view).
     */
    @Bean
    PgconfigGwcCatalogRenameListener pgconfigGwcCatalogRenameListener(
            @Qualifier("rawCatalog") Catalog catalog, ApplicationEventPublisher eventPublisher) {
        return new PgconfigGwcCatalogRenameListener(catalog, eventPublisher::publishEvent);
    }

    /**
     * The tile layer catalog. Its lookups and listings take the layers and groups from the facade of the raw catalog. A
     * listing puts rows in the entity cache of the catalog up to half of its maximum weight.
     */
    @Bean(name = "gwcCatalogConfiguration")
    GeoServerTileLayerConfiguration pgconfigTileLayerCatalog(
            GridSetBroker gridsetBroker,
            TileLayerInfoRepository repository,
            ApplicationEventPublisher eventPublisher,
            @Qualifier("rawCatalog") CatalogPlugin rawCatalog,
            GWCConfigPersister defaultsProvider,
            BuiltTileLayerCache builtTileLayers,
            @Qualifier("pgconfigCatalogEntityCache") ObjectProvider<EntityCache> catalogCache) {

        Supplier<PutBudget> listingBudget = listingBudget(catalogCache);
        PgconfigTileLayerCatalog config = new PgconfigTileLayerCatalog(
                repository, gridsetBroker, rawCatalog::getFacade, defaultsProvider, builtTileLayers, listingBudget);
        Consumer<TileLayerEvent> gwcEventPublisher = eventPublisher::publishEvent;
        return new GeoServerTileLayerConfiguration(config, gwcEventPublisher);
    }

    /** Gives each listing a new budget of half the entity cache, and an unlimited one without the cache. */
    private static Supplier<PutBudget> listingBudget(ObjectProvider<EntityCache> catalogCache) {
        EntityCache cache = catalogCache.getIfAvailable();
        if (null == cache) {
            return PutBudget::unlimited;
        }
        return () -> PutBudget.halfOf(cache);
    }

    /**
     * Evicts the cached GWC settings of a tile layer on its {@link TileLayerEvent}s, and makes the requests running
     * during a change made through another instance read the settings again.
     */
    @Bean
    PgconfigTileLayerEventListener pgconfigTileLayerEventListener(
            @Qualifier("pgconfigCatalogEntityCache") ObjectProvider<EntityCache> catalogCache) {

        EntityCache cache = catalogCache.getIfAvailable();
        return new PgconfigTileLayerEventListener(cache, RemoteChangeCounter.shared());
    }

    /**
     * The tile layers found by name, kept until the catalog changes: at most
     * {@code geoserver.backend.pgconfig.cache.tile-layers} of them. If {@code geoserver.backend.pgconfig.cache.enabled}
     * is false, the catalog has no entity cache, and lookups build their tile layers.
     */
    @Bean
    BuiltTileLayerCache pgconfigBuiltTileLayerCache(
            @Qualifier("pgconfigDataSource") DataSource dataSource,
            @Qualifier("pgconfigCatalogEntityCache") ObjectProvider<EntityCache> catalogCache,
            EntityCacheProperties cacheProperties) {

        EntityCache cache = catalogCache.getIfAvailable();
        int maximumSize = cacheProperties.getTileLayers();
        return new BuiltTileLayerCache(cache, dataSource, maximumSize);
    }

    /**
     * Registers the metrics of the kept tile layers, as for the entity caches. A nested class keeps the Micrometer
     * types out of the methods of the outer configuration: Spring reads those methods, and fails on a type missing from
     * the classpath.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    static class BuiltTileLayerCacheMetricsConfiguration {

        @Bean
        @ConditionalOnPgconfigCacheEnabled
        EntityCacheMetrics pgconfigBuiltTileLayerCacheMetrics(BuiltTileLayerCache builtTileLayers) {
            return new EntityCacheMetrics(BuiltTileLayerCache.NAME, builtTileLayers.caffeine());
        }
    }

    /**
     * The GWC settings of the tile layers, read once per request. They come from the entity cache of the catalog, next
     * to their layers and groups, unless {@code geoserver.backend.pgconfig.cache.enabled} is false.
     */
    @Bean
    TileLayerInfoRepository pgconfigTileLayerRepository(
            @Qualifier("pgconfigDataSource") DataSource dataSource,
            @Qualifier("pgconfigCatalogEntityCache") ObjectProvider<EntityCache> catalogCache) {

        JdbcTemplate template = new JdbcTemplate(dataSource);
        EntityCache cache = catalogCache.getIfAvailable();
        PgconfigTileLayerInfoRepository repository = new PgconfigTileLayerInfoRepository(template, cache);
        return new RequestCachingTileLayerInfoRepository(repository);
    }
}
