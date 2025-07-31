/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.features;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.configuration.extension.ogcapi.features.OgcApiFeaturesWebUIConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @see OgcApiFeaturesWebUIConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnOgcApiFeatures
@ConditionalOnGeoServerWebUI
@EnableConfigurationProperties(OgcApiFeatureConfigProperties.class)
@Import(OgcApiFeaturesWebUIConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.ogcapi.features")
class OgcApiFeaturesWebUIAutoConfiguration {

    @PostConstruct
    void log() {
        log.info("OGC API Features WEBUI extension enabled");
    }
}
