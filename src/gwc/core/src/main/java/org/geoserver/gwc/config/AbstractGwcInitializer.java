/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerReinitializer;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geowebcache.locks.LockProvider;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;

/**
 * Base class for replacements of {@link GWCInitializer}.
 *
 * <p>This is required because GeoServer Cloud may not set up a {@link TileLayerCatalog}, which
 * {@link GWCInitializer} requires.
 *
 * <p>This {@link GeoServerReinitializer} is hence in charge of notifying {@link
 * ConfigurableBlobStore#setChanged(org.geoserver.gwc.config.GWCConfig, boolean)}
 *
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
}
