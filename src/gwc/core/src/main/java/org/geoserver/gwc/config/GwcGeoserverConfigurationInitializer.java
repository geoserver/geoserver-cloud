/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.gwc.config;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;

import java.io.IOException;

import javax.annotation.PostConstruct;

/**
 * Replaces {@link GWCInitializer}
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
@Slf4j
@RequiredArgsConstructor
public class GwcGeoserverConfigurationInitializer {

    @NonNull private final GWCConfigPersister configPersister;
    @NonNull private final GeoServerConfigurationLock configLock;

    @PostConstruct
    void initialize() throws IOException {
        initializeGeoServerIntegrationConfigFile();
    }

    private void initializeGeoServerIntegrationConfigFile() throws IOException {
        if (configExists()) {
            return;
        }
        final boolean lockAcquired = configLock.tryLock(LockType.WRITE);
        if (lockAcquired) {
            try {
                if (!configExists()) {
                    log.info("Initializing GeoServer specific GWC configuration");
                    GWCConfig defaults = new GWCConfig();
                    defaults.setVersion("1.1.0");
                    configPersister.save(defaults);
                }
            } finally {
                configLock.unlock();
            }
        }
    }

    private boolean configExists() throws IOException {
        Resource configFile = configPersister.findConfigFile();
        return Resources.exists(configFile);
    }
}
