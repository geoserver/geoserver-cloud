/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.backend;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.client.util.Objects;
import com.google.common.base.Stopwatch;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.gwc.backend.pgconfig.PgconfigTileLayerCatalog;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.cloud.gwc.repository.GeoServerTileLayerConfiguration;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerReinitializer;
import org.geoserver.gwc.ConfigurableBlobStore;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.gwc.config.GWCInitializer;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geowebcache.config.TileLayerConfiguration;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration;
import org.geowebcache.storage.blobstore.memory.CacheProvider;
import org.geowebcache.storage.blobstore.memory.guava.GuavaCacheProvider;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Replacement for {@link GWCInitializer} when using the "pgconfig" storage.
 *
 * <p>This is required because {@link PgconfigTileLayerCatalogAutoConfiguration} does not set up a
 * {@link TileLayerCatalog}, which {@link GWCInitializer} requires, but a {@link
 * PgconfigTileLayerCatalog} instead, which directly implements {@link TileLayerConfiguration} to be
 * contributed to {@link TileLayerDispatcher}.
 *
 * <p>This {@link GeoServerReinitializer} is hence in charge of notifying {@link
 * ConfigurableBlobStore#setChanged(org.geoserver.gwc.config.GWCConfig, boolean)}
 *
 * <p>This bean also listens to {@link TileLayerEvent}s and notifies the {@link CacheProvider} to
 * either {@link CacheProvider#removeLayer(String) removeLayer} or {@link
 * CacheProvider#removeUncachedLayer(String) removeUncachedLayer} on changes and deletions as
 * appropriate, or to {@link CacheProvider#addUncachedLayer(String) addUncachedLayer} when a layer
 * is created to changed to not be memory cached.
 *
 * @since 1.8
 */
@RequiredArgsConstructor
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.backend")
class PgconfigGwcInitializer implements GeoServerReinitializer {

    private final @NonNull GWCConfigPersister configPersister;
    private final @NonNull ConfigurableBlobStore blobStore;
    private final @NonNull GeoServerTileLayerConfiguration geoseverTileLayers;

    /**
     * @see org.geoserver.config.GeoServerInitializer#initialize(org.geoserver.config.GeoServer)
     */
    @Override
    public void initialize(final GeoServer geoServer) throws Exception {
        log.info("Initializing GeoServer specific GWC configuration from gwc-gs.xml");

        final GWCConfig gwcConfig = configPersister.getConfig();
        checkNotNull(gwcConfig);

        configureInMemoryCacheProvider(gwcConfig);

        final boolean initialization = true;
        blobStore.setChanged(gwcConfig, initialization);

        setUpNonMemoryCacheableLayers();
    }

    @EventListener(TileLayerEvent.class)
    void onTileLayerEvent(TileLayerEvent event) {
        cacheProvider()
                .ifPresent(
                        cache -> {
                            switch (event.getEventType()) {
                                case DELETED:
                                    log.debug(
                                            "TileLayer {} deleted, notifying in-memory CacheProvider",
                                            event.getName());
                                    cache.removeUncachedLayer(event.getName());
                                    break;
                                case MODIFIED:
                                    if (event.getOldName() != null
                                            && !Objects.equal(
                                                    event.getOldName(), event.getName())) {
                                        log.info(
                                                "TileLayer {} renamed to {}, notifying in-memory CacheProvider",
                                                event.getOldName(),
                                                event.getName());
                                        cache.removeUncachedLayer(event.getOldName());
                                    }
                                    setInMemoryLayerCaching(event.getName());
                                    break;
                                default:
                                    setInMemoryLayerCaching(event.getName());
                                    break;
                            }
                        });
    }

    private void configureInMemoryCacheProvider(final GWCConfig gwcConfig) throws IOException {
        // Setting default CacheProvider class if not present
        if (gwcConfig.getCacheProviderClass() == null
                || gwcConfig.getCacheProviderClass().isEmpty()) {
            gwcConfig.setCacheProviderClass(GuavaCacheProvider.class.toString());
            configPersister.save(gwcConfig);
        }

        // Setting default Cache Configuration
        if (gwcConfig.getCacheConfigurations() == null) {
            log.debug("Setting default CacheConfiguration");
            Map<String, CacheConfiguration> map = new HashMap<>();
            map.put(GuavaCacheProvider.class.toString(), new CacheConfiguration());
            gwcConfig.setCacheConfigurations(map);
            configPersister.save(gwcConfig);
        } else {
            log.debug("CacheConfiguration loaded");
        }

        // Change ConfigurableBlobStore behavior
        String cacheProviderClass = gwcConfig.getCacheProviderClass();
        Map<String, CacheProvider> cacheProviders = blobStore.getCacheProviders();
        if (!cacheProviders.containsKey(cacheProviderClass)) {
            gwcConfig.setCacheProviderClass(GuavaCacheProvider.class.toString());
            configPersister.save(gwcConfig);
            log.debug("Unable to find: {}, used default configuration", cacheProviderClass);
        }
    }

    private void setInMemoryLayerCaching(@NonNull String layerName) {

        layer(layerName)
                .ifPresentOrElse(this::addUncachedLayer, () -> removeCachedLayer(layerName));
    }

    private void removeCachedLayer(String layerName) {
        cacheProvider()
                .ifPresent(
                        cache -> {
                            log.debug(
                                    "TileLayer {} does not exist, notifying CacheProvider",
                                    layerName);
                            cache.removeLayer(layerName);
                            cache.removeUncachedLayer(layerName);
                        });
    }

    private void addUncachedLayer(GeoServerTileLayer tl) {
        if (!tl.getInfo().isInMemoryCached()) {
            log.debug(
                    "TileLayer {} is not to be memory cached, notifying CacheProvider",
                    tl.getName());
            cacheProvider().ifPresent(cache -> cache.addUncachedLayer(tl.getName()));
        }
    }

    private Optional<GeoServerTileLayer> layer(String layerName) {
        return geoseverTileLayers
                .getLayer(layerName)
                .filter(GeoServerTileLayer.class::isInstance)
                .map(GeoServerTileLayer.class::cast);
    }

    /**
     * Private method for adding all the Layer that must not be cached to the {@link CacheProvider}
     * instance.
     */
    private void setUpNonMemoryCacheableLayers() {
        cacheProvider()
                .ifPresent(
                        cache -> {
                            // Add all the various Layers to avoid caching
                            log.info("Adding Layers to avoid In Memory Caching");
                            // it is ok to use the ForkJoinPool.commonPool() here, there's no I/O
                            // involved
                            Stopwatch sw = Stopwatch.createStarted();
                            Collection<? extends TileLayer> layers = geoseverTileLayers.getLayers();
                            log.info("Queried {} tile layers in {}", layers.size(), sw.stop());
                            layers.stream()
                                    .filter(GeoServerTileLayer.class::isInstance)
                                    .map(GeoServerTileLayer.class::cast)
                                    .filter(l -> l.isEnabled() && !l.getInfo().isInMemoryCached())
                                    .map(GeoServerTileLayer::getName)
                                    .forEach(cache::addUncachedLayer);
                        });
    }

    private Optional<CacheProvider> cacheProvider() {
        return Optional.ofNullable(blobStore.getCache());
    }
}
