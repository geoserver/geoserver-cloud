/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.backend;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.cloud.gwc.backend.pgconfig.PgconfigTileLayerCatalog;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.cloud.gwc.repository.GeoServerTileLayerConfiguration;
import org.geoserver.config.GeoServerReinitializer;
import org.geoserver.gwc.ConfigurableBlobStore;
import org.geoserver.gwc.config.AbstractGwcInitializer;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.gwc.config.GWCInitializer;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geowebcache.config.TileLayerConfiguration;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.storage.blobstore.memory.CacheProvider;
import org.slf4j.Logger;

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
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.backend")
class PgconfigGwcInitializer extends AbstractGwcInitializer {

    public PgconfigGwcInitializer(
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
