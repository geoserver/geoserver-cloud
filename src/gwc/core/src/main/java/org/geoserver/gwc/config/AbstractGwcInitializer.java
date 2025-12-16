/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.config;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.cloud.gwc.repository.GeoServerTileLayerConfiguration;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerReinitializer;
import org.geoserver.gwc.ConfigurableBlobStore;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.locks.LockProvider;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration;
import org.geowebcache.storage.blobstore.memory.CacheProvider;
import org.geowebcache.storage.blobstore.memory.guava.GuavaCacheProvider;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;

/**
 * Base class for replacements of {@link GWCInitializer}.
 *
 * <p>This is required because GeoServer Cloud may not set up a {@link TileLayerCatalog}, which
 * {@link GWCInitializer} requires.
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
 * @see DefaultGwcInitializer
 * @see {@code PgconfigGwcInitializer}
 * @since 1.8
 */
@RequiredArgsConstructor
public abstract class AbstractGwcInitializer implements GeoServerReinitializer, InitializingBean {

    /**
     * {@link GWC#saveConfig(GWCConfig)} will lookup for the {@link LockProvider} named after {@link
     * GWCConfig#getLockProviderName()}. We need it to be a cluster-aware lock provider. This is the
     * bean name to be registered by the configuration, and we'll set it to {@link
     * GWCConfig#setLockProviderName(String)} during initialization.
     */
    public static final String GWC_LOCK_PROVIDER_BEAN_NAME = "gwcClusteringLockProvider";

    protected final @NonNull GWCConfigPersister configPersister;
    protected final @NonNull ConfigurableBlobStore blobStore;
    protected final @NonNull GeoServerTileLayerConfiguration geoseverTileLayers;
    protected final @NonNull GeoServerConfigurationLock globalConfigLock;

    protected abstract Logger logger();

    @Override
    public void afterPropertiesSet() throws IOException {
        initializeGeoServerIntegrationConfigFile();
    }

    /**
     * @see org.geoserver.config.GeoServerInitializer#initialize(org.geoserver.config.GeoServer)
     */
    @Override
    public void initialize(final GeoServer geoServer) throws Exception {
        logger().info("Initializing GeoServer specific GWC configuration from gwc-gs.xml");

        final GWCConfig gwcConfig = configPersister.getConfig();
        checkNotNull(gwcConfig);

        configureInMemoryCacheProvider(gwcConfig);

        final boolean initialization = true;
        blobStore.setChanged(gwcConfig, initialization);

        setUpNonMemoryCacheableLayers();
    }

    /**
     * Initialize the datadir/gs-gwc.xml file before {@link
     * #initialize(org.geoserver.config.GeoServer) super.initialize(GeoServer)}
     */
    private void initializeGeoServerIntegrationConfigFile() throws IOException {
        globalConfigLock.lock(LockType.WRITE);
        try {
            if (configFileExists()) {
                updateLockProviderName();
            } else {
                logger().info("Initializing GeoServer specific GWC configuration {}", configPersister.findConfigFile());
                GWCConfig defaults = new GWCConfig();
                defaults.setVersion("1.1.0");
                defaults.setLockProviderName(GWC_LOCK_PROVIDER_BEAN_NAME);
                configPersister.save(defaults);
            }
        } finally {
            globalConfigLock.unlock();
        }
    }

    /**
     * In case the {@link GWCConfig} exists and its lock provider name is not {@link
     * #GWC_LOCK_PROVIDER_BEAN_NAME}, updates and saves the configuration.
     *
     * <p>At this point, {@link #configFileExists()} is known to be true.
     */
    private void updateLockProviderName() throws IOException {
        final GWCConfig gwcConfig = configPersister.getConfig();
        if (!GWC_LOCK_PROVIDER_BEAN_NAME.equals(gwcConfig.getLockProviderName())) {
            if (null == gwcConfig.getLockProviderName()) {
                logger().info("Setting GeoWebCache lock provider to {}", GWC_LOCK_PROVIDER_BEAN_NAME);
            } else {
                logger().warn(
                                "Updating GeoWebCache lock provider from {} to {}",
                                gwcConfig.getLockProviderName(),
                                GWC_LOCK_PROVIDER_BEAN_NAME);
            }
            gwcConfig.setLockProviderName(GWC_LOCK_PROVIDER_BEAN_NAME);
            configPersister.save(gwcConfig);
        }
    }

    private boolean configFileExists() throws IOException {
        Resource configFile = configPersister.findConfigFile();
        return Resources.exists(configFile);
    }

    @EventListener(TileLayerEvent.class)
    void onTileLayerEvent(TileLayerEvent event) {
        cacheProvider().ifPresent(cache -> onTileLayerEvent(event, cache));
    }

    private void onTileLayerEvent(TileLayerEvent event, CacheProvider cache) {
        @NonNull String layerName = event.getName();

        switch (event.getEventType()) {
            case DELETED:
                logger().debug("TileLayer {} deleted, notifying in-memory CacheProvider", layerName);
                cache.removeUncachedLayer(layerName);
                break;
            case MODIFIED:
                if (isRename(event)) {
                    String oldName = event.getOldName();
                    logger().info("TileLayer {} renamed to {}, notifying in-memory CacheProvider", oldName, layerName);
                    cache.removeUncachedLayer(oldName);
                }
                setInMemoryLayerCaching(layerName);
                break;
            default:
                setInMemoryLayerCaching(layerName);
                break;
        }
    }

    private boolean isRename(TileLayerEvent event) {
        String layerName = event.getName();
        String oldName = event.getOldName();
        return oldName != null && !Objects.equals(oldName, layerName);
    }

    private void configureInMemoryCacheProvider(final GWCConfig gwcConfig) throws IOException {
        // Setting default CacheProvider class if not present
        if (!StringUtils.hasText(gwcConfig.getCacheProviderClass())) {
            gwcConfig.setCacheProviderClass(GuavaCacheProvider.class.toString());
            configPersister.save(gwcConfig);
        }

        // Setting default Cache Configuration
        if (gwcConfig.getCacheConfigurations() == null) {
            logger().debug("Setting default CacheConfiguration");
            Map<String, CacheConfiguration> map = new HashMap<>();
            map.put(GuavaCacheProvider.class.toString(), new CacheConfiguration());
            gwcConfig.setCacheConfigurations(map);
            configPersister.save(gwcConfig);
        } else {
            logger().debug("CacheConfiguration loaded");
        }

        // Change ConfigurableBlobStore behavior
        String cacheProviderClass = gwcConfig.getCacheProviderClass();
        Map<String, CacheProvider> cacheProviders = blobStore.getCacheProviders();
        if (!cacheProviders.containsKey(cacheProviderClass)) {
            gwcConfig.setCacheProviderClass(GuavaCacheProvider.class.toString());
            configPersister.save(gwcConfig);
            logger().debug("Unable to find: {}, used default configuration", cacheProviderClass);
        }
    }

    private void setInMemoryLayerCaching(@NonNull String layerName) {

        layer(layerName).ifPresentOrElse(this::addUncachedLayer, () -> removeCachedLayer(layerName));
    }

    private void removeCachedLayer(String layerName) {
        cacheProvider().ifPresent(cache -> {
            logger().debug("TileLayer {} does not exist, notifying CacheProvider", layerName);
            cache.removeLayer(layerName);
            cache.removeUncachedLayer(layerName);
        });
    }

    private void addUncachedLayer(GeoServerTileLayer tl) {
        if (!tl.getInfo().isInMemoryCached()) {
            logger().debug("TileLayer {} is not to be memory cached, notifying CacheProvider", tl.getName());
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
        cacheProvider().ifPresent(cache -> {
            // Add all the various Layers to avoid caching
            logger().info("Adding Layers to avoid In Memory Caching");
            // it is ok to use the ForkJoinPool.commonPool() here, there's no I/O
            // involved
            Stopwatch sw = Stopwatch.createStarted();
            Collection<? extends TileLayer> layers = geoseverTileLayers.getLayers();
            logger().info("Queried {} tile layers in {}", layers.size(), sw.stop());
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
