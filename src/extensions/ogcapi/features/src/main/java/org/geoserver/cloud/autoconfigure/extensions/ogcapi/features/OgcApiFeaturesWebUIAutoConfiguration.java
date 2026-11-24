/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.features;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.configuration.extension.ogcapi.features.OgcApiFeaturesWebConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Registers the web UI side of the OGC API Features extension.
 *
 * <p>Ordered after Spring Boot's Web MVC auto-configuration for the reason given in
 * {@link OgcApiFeaturesAutoConfiguration}: this chain reaches {@code APIConfigurationSupport} too, and must not
 * register it before Boot has decided who owns Spring MVC.
 *
 * @see OgcApiFeaturesWebConfiguration
 */
@AutoConfiguration(afterName = "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration")
@ConditionalOnOgcApiFeatures
@ConditionalOnGeoServerWebUI
@Import(OgcApiFeaturesWebConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.ogcapi.features")
class OgcApiFeaturesWebUIAutoConfiguration {

    @PostConstruct
    void log() {
        log.info("OGC API Features WEBUI extension enabled");
    }
}
