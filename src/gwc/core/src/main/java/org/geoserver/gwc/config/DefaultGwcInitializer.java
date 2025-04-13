/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.config;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.cloud.gwc.repository.GeoServerTileLayerConfiguration;
import org.geoserver.gwc.ConfigurableBlobStore;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.slf4j.Logger;

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
public class DefaultGwcInitializer extends AbstractGwcInitializer {

    public DefaultGwcInitializer(
            @NonNull GWCConfigPersister configPersister,
            @NonNull ConfigurableBlobStore blobStore,
            @NonNull GeoServerTileLayerConfiguration geoseverTileLayers,
            @NonNull GeoServerConfigurationLock configLock) {

        super(configPersister, blobStore, geoseverTileLayers, configLock);
    }

    @Override
    protected Logger logger() {
        return log;
    }
}
