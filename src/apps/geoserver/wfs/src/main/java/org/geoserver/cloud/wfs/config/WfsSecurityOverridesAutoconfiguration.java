/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wfs.config;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.autoconfigure.catalog.backend.core.GeoServerBackendAutoConfiguration;
import org.geoserver.cloud.wfs.security.NoopLayerGroupContainmentCache;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Runs before {@link GeoServerBackendAutoConfiguration} to provide a {@link
 * NoopLayerGroupContainmentCache} before {@link CoreBackendConfiguration's} {@code
 * layerGroupContainmentCache()} does.
 *
 * @since 1.8.2
 */
@AutoConfiguration(before = GeoServerBackendAutoConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.wfs.config")
public class WfsSecurityOverridesAutoconfiguration {

    @Bean
    LayerGroupContainmentCache layerGroupContainmentCache() {
        log.info("wfs-service is using a no-op LayerGroupContainmentCache");
        return new NoopLayerGroupContainmentCache();
    }
}
