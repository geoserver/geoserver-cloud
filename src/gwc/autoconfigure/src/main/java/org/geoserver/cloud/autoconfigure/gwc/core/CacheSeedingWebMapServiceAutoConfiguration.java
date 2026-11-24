/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.core;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.autoconfigure.gwc.core.CacheSeedingWebMapServiceAutoConfiguration.MinimalWebMapServiceAutoConfiguration;
import org.geoserver.cloud.gwc.config.core.WebMapServiceCacheSeedingConfiguration;
import org.geoserver.configuration.gwc.GwcWMSMinimalConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.WebMapService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto configuration} to make sure a minimal {@link WebMapService} and the
 * GeoWebCache-specific decorator exist, as expected by {@link GeoServerTileLayer#seedTile}.
 *
 * <p>Follows {@link ConditionalOnGeoWebCacheEnabled} into every role that hosts GeoWebCache. Seeding is not the only
 * consumer: the GWC mediator is built with a {@code coalescedRequestSplitter} that renders the non-cached members of a
 * multi-layer tile request through the {@code wmsGetMap} bean, which this minimal WMS supplies wherever the WMS service
 * itself is absent. Which roles host GeoWebCache at all is decided by that condition, keeping the WMS beans out of the
 * roles that never had them.
 *
 * @since 1.0
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnGeoWebCacheEnabled
@Import({MinimalWebMapServiceAutoConfiguration.class, WebMapServiceCacheSeedingConfiguration.class})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.integration")
public class CacheSeedingWebMapServiceAutoConfiguration {

    public @PostConstruct void log() {
        log.info("GeoWebCache WMS decorator for seeding enabled");
    }

    @Configuration
    @ConditionalOnMissingBean(DefaultWebMapService.class)
    @Import(GwcWMSMinimalConfiguration.class)
    static class MinimalWebMapServiceAutoConfiguration {

        MinimalWebMapServiceAutoConfiguration() {
            log.info("Loaded a default MinimalWebMapServiceAutoConfiguration");
        }
    }
}
