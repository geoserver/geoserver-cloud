/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.features;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.cloud.configuration.ogcapi.core.OgcApiCoreConfiguration;
import org.geoserver.cloud.configuration.ogcapi.core.OgcApiCoreWebConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnOgcApiFeatures
@ConditionalOnGeoServerWebUI
@Import({OgcApiCoreConfiguration.class, OgcApiCoreWebConfiguration.class})
@ImportFilteredResource({
    "jar:gs-ogcapi-features-.*!/applicationContext.xml",
    "jar:gs-web-features-.*!/applicationContext.xml"
})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.ogcapi.features")
class OgcApiFeaturesWebUIConfiguration {

    @PostConstruct
    void log() {
        log.info("OGC API Features WEBUI extension enabled");
    }
}
