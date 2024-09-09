/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.gwc.config;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.cloud.gwc.config.core.AbstractGwcInitializer;
import org.geoserver.cloud.gwc.repository.GeoServerTileLayerConfiguration;
import org.geoserver.gwc.ConfigurableBlobStore;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;

/**
 * Replaces {@link GWCInitializer}
 *
 * <p>Using package {@code org.geoserver.gwc.config} to be able of accessing the package-private
 * method {@link GWCConfigPersister#findConfigFile()}
 *
 * <p>
 *
 * <ul>
 *   <li>We don't need to upgrade from very old configuration settings
 *   <li>{@code GWCInitializer} depends on {@link TileLayerCatalog}, assuming {@link
 *       CatalogConfiguration} is the only tile layer storage backend for geoserver tile layers, and
 *       it's not the case for GS cloud
 * </ul>
 */
@Slf4j(topic = "org.geoserver.cloud.gwc.config.core")
public class DefaultGwcInitializer extends AbstractGwcInitializer implements InitializingBean {

    @NonNull private final GeoServerConfigurationLock configLock;

    public DefaultGwcInitializer(
            @NonNull GWCConfigPersister configPersister,
            @NonNull ConfigurableBlobStore blobStore,
            @NonNull GeoServerTileLayerConfiguration geoseverTileLayers,
            @NonNull GeoServerConfigurationLock configLock) {

        super(configPersister, blobStore, geoseverTileLayers);
        this.configLock = configLock;
    }

    @Override
    protected Logger logger() {
        return log;
    }

    /**
     * Initialize the datadir/gs-gwc.xml file before {@link
     * #initialize(org.geoserver.config.GeoServer) super.initialize(GeoServer)}
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        initializeGeoServerIntegrationConfigFile();
    }

    private void initializeGeoServerIntegrationConfigFile() throws IOException {
        if (configFileExists()) {
            return;
        }
        final boolean lockAcquired = configLock.tryLock(LockType.WRITE);
        if (lockAcquired) {
            try {
                if (!configFileExists()) {
                    log.info(
                            "Initializing GeoServer specific GWC configuration {}",
                            configPersister.findConfigFile());
                    GWCConfig defaults = new GWCConfig();
                    defaults.setVersion("1.1.0");
                    configPersister.save(defaults);
                }
            } finally {
                configLock.unlock();
            }
        }
    }

    private boolean configFileExists() throws IOException {
        Resource configFile = configPersister.findConfigFile();
        return Resources.exists(configFile);
    }
}
