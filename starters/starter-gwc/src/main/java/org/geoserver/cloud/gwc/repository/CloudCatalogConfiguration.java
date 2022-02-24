/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.repository;

import com.google.common.cache.LoadingCache;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geowebcache.grid.GridSetBroker;
import org.springframework.context.event.EventListener;

/**
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.gwc.repository")
public class CloudCatalogConfiguration extends CatalogConfiguration {

    private LoadingCache<String, GeoServerTileLayer> spiedLayerCache;

    @SuppressWarnings("unchecked")
    public CloudCatalogConfiguration(
            Catalog catalog, TileLayerCatalog tileLayerCatalog, GridSetBroker gridSetBroker) {

        super(catalog, tileLayerCatalog, gridSetBroker);

        try {
            spiedLayerCache =
                    (LoadingCache<String, GeoServerTileLayer>)
                            FieldUtils.readField(this, "layerCache", true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @EventListener(TileLayerEvent.class)
    public void onTileLayerEvent(TileLayerEvent event) {
        log.debug("evicting GeoServerTileLayer cache entry upon {}", event);
        spiedLayerCache.invalidate(event.getLayerId());
    }
}
