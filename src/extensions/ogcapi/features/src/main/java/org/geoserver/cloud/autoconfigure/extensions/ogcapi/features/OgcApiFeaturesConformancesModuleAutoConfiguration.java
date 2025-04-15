/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.features;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.geoserver.jackson.databind.ogcapi.features.OgcApiFeaturesConformancesModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Contributes an {@link OgcApiFeaturesConformancesModule} to the default spring boot {@link ObjectMapper}, required
 * to serialize/deserialize conformance classes in spring cloud bus events
 */
@AutoConfiguration
public class OgcApiFeaturesConformancesModuleAutoConfiguration {

    @Bean
    OgcApiFeaturesConformancesModule ogcApiFeaturesConformancesModule() {
        return new OgcApiFeaturesConformancesModule();
    }
}
