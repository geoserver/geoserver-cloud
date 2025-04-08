/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.core;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.autoconfigure.gwc.backend.DefaultTileLayerCatalogAutoConfiguration;
import org.geoserver.cloud.gwc.config.core.GeoServerIntegrationConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-Configuration} to integrated GeoServer-specific GWC
 * extensions, for example, to being able of configuring tile layers out of GeoServer Layers.
 *
 * @see ConditionalOnGeoWebCacheEnabled
 * @see GeoServerIntegrationConfiguration
 * @see DefaultTileLayerCatalogAutoConfiguration
 * @since 1.0
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnGeoWebCacheEnabled
@Import({GeoServerIntegrationConfiguration.class})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.core")
public class GeoServerIntegrationAutoConfiguration {

    @PostConstruct
    public void log() {
        log.info("GeoWebCache core GeoServer integration enabled");
    }
}
