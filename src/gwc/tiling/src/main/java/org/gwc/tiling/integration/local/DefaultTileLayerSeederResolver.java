/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.integration.local;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.gwc.tiling.model.CacheIdentifier;
import org.gwc.tiling.service.TileLayerSeeder;

import java.util.Optional;
import java.util.function.Function;

/**
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultTileLayerSeederResolver
        implements Function<CacheIdentifier, Optional<TileLayerSeeder>> {

    private final @NonNull TileLayerDispatcher tld;

    @Override
    public Optional<TileLayerSeeder> apply(CacheIdentifier cacheId) {
        String layerName = cacheId.getLayerName();
        TileLayer tileLayer = null;
        TileLayerSeeder layerSeeder = null;

        try {
            tileLayer = tld.getTileLayer(layerName);
        } catch (GeoWebCacheException e) {
            log.warn("Layer not found: {} ({})", layerName, e.getMessage());
        }
        if (tileLayer instanceof GeoServerTileLayer) {
            layerSeeder = new GeoServerTileLayerSeeder((GeoServerTileLayer) tileLayer);
        }
        return Optional.ofNullable(layerSeeder);
    }
}
