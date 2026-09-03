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

/**
 * Configuration class for the OGC API Features administration pages, conditional on the web UI service.
 * <p>
 * {@code featureServiceXStreamInitializer} is filtered out: every service
 * reads and writes the conformance objects it describes, so it is contributed
 * unconditionally by
 * {@link OgcApiFeaturesXStreamPersisterAutoConfiguration} instead of by this
 * service-scoped configuration.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnOgcApiFeatures
@ConditionalOnGeoServerWebUI
@Import({OgcApiCoreConfiguration.class, OgcApiCoreWebConfiguration.class})
@ImportFilteredResource({
    "jar:gs-ogcapi-features-.*!/applicationContext.xml#name=^(?!featureServiceXStreamInitializer).*$",
    "jar:gs-web-features-.*!/applicationContext.xml"
})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.ogcapi.features")
class OgcApiFeaturesWebUIConfiguration {

    @PostConstruct
    void log() {
        log.info("OGC API Features WEBUI extension enabled");
    }
}
