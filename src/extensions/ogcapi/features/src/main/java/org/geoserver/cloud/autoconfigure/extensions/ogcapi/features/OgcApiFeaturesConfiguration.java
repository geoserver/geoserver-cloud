/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.features;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWFS;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.cloud.configuration.ogcapi.core.OgcApiCoreConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class for OGC API Features, conditional on WFS service.
 */
@Configuration
@ConditionalOnOgcApiFeatures
@ConditionalOnGeoServerWFS
@Import(OgcApiCoreConfiguration.class)
@ImportFilteredResource("jar:gs-ogcapi-features-.*!/applicationContext.xml")
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.ogcapi.features")
class OgcApiFeaturesConfiguration {

    @PostConstruct
    void log() {
        log.info("OGC API Features extension enabled");
    }
}
