/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.backend;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.gwc.config.core.DefaultTileLayerCatalogConfiguration;
import org.geoserver.cloud.gwc.repository.GeoServerTileLayerConfiguration;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;

/**
 * {@link AutoConfiguration @AutoConfiguration} to set up the GeoServer {@link TileLayerCatalog}
 * using the default implementation based on the {@link ResourceStore}
 *
 * @see DefaultTileLayerCatalogConfiguration
 * @see ConditionalOnGeoWebCacheEnabled
 * @since 1.0
 */
@AutoConfiguration(after = PgsqlTileLayerCatalogAutoConfiguration.class)
@ConditionalOnGeoWebCacheEnabled
@ConditionalOnMissingBean(GeoServerTileLayerConfiguration.class)
@Import(DefaultTileLayerCatalogConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.backend")
public class DefaultTileLayerCatalogAutoConfiguration {

    @PostConstruct
    void log() {
        log.info("GeoWebCache TileLayerCatalog using default ResourceStore config backend");
    }
}
