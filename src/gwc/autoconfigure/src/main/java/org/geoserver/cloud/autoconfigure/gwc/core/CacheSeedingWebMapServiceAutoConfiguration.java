/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.core;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.autoconfigure.gwc.core.CacheSeedingWebMapServiceAutoConfiguration.MinimalWebMapServiceAutoConfiguration;
import org.geoserver.cloud.gwc.config.core.WebMapServiceCacheSeedingConfiguration;
import org.geoserver.configuration.core.wms.WMSCoreMinimalConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.WebMapService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto configuration} to make sure a minimal {@link WebMapService}
 * and the GeoWebCache-specific decorator exist, as expected by {@link GeoServerTileLayer#seedTile}.
 *
 * @since 1.0
 * @see WMSCoreMinimalConfiguration
 * @see WebMapServiceCacheSeedingConfiguration
 */
@AutoConfiguration
@ConditionalOnGeoWebCacheEnabled
@Import({MinimalWebMapServiceAutoConfiguration.class, WebMapServiceCacheSeedingConfiguration.class})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.integration")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class CacheSeedingWebMapServiceAutoConfiguration {

    public @PostConstruct void log() {
        log.info("GeoWebCache WMS decorator for seeding enabled");
    }

    /**
     * Conditional configuration to include a {@link WMSCoreMinimalConfiguration minimal WMS service}
     * for GeoWebcache if there's no {@link DefaultWebMapService} already in the application context.
     */
    @Configuration
    @ConditionalOnMissingBean(DefaultWebMapService.class)
    @Import(WMSCoreMinimalConfiguration.class)
    static class MinimalWebMapServiceAutoConfiguration {}
}
