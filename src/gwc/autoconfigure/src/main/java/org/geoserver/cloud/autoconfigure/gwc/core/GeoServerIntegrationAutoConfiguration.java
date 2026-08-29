/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.core;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.autoconfigure.gwc.backend.DefaultTileLayerCatalogAutoConfiguration;
import org.geoserver.cloud.gwc.event.ConfigChangeEvent;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.configuration.gwc.GwcGeoServerContextConfiguration;
import org.geoserver.configuration.gwc.GwcImageCodecsConfiguration;
import org.geoserver.gwc.config.CloudGwcConfigPersister;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-Configuration} to integrate GeoServer-specific GWC extensions, for example, to
 * being able of configuring tile layers out of GeoServer Layers.
 *
 * @see ConditionalOnGeoWebCacheEnabled
 * @see GwcGeoServerContextConfiguration
 * @see GwcImageCodecsConfiguration
 * @see DefaultTileLayerCatalogAutoConfiguration
 * @since 1.0
 */
@AutoConfiguration
@ConditionalOnGeoWebCacheEnabled
@Import({GwcGeoServerContextConfiguration.class, GwcImageCodecsConfiguration.class})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.core")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class GeoServerIntegrationAutoConfiguration {

    @PostConstruct
    public void log() {
        log.info("GeoWebCache core GeoServer integration enabled");
    }

    /**
     * Overrides {@code gwcGeoServervConfigPersister} with a cluster-aware {@link CloudGwcConfigPersister} that sends
     * {@link ConfigChangeEvent}s upon {@link GWCConfigPersister#save(org.geoserver.gwc.config.GWCConfig)}
     *
     * @param xsfp
     * @param resourceLoader
     */
    @Bean
    GWCConfigPersister gwcGeoServervConfigPersister(
            XStreamPersisterFactory xsfp, GeoServerResourceLoader resourceLoader, ApplicationEventPublisher publisher) {
        return new CloudGwcConfigPersister(xsfp, resourceLoader, publisher::publishEvent);
    }
}
