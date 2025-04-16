/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.features;

import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for OGC API Features extension.
 * <p>
 * This auto-configuration class is designed to set up the OGC API Features
 * extension in GeoServer Cloud. It consists of:
 * <ul>
 * <li>Core configuration class that imports the core
 * applicationContext.xml</li>
 * <li>REST configuration class that imports the features
 * applicationContext.xml</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(OgcApiFeatureConfigProperties.class)
@ConditionalOnOgcApiFeatures
@Import({OgcApiFeaturesConfiguration.class, OgcApiFeaturesWebUIConfiguration.class})
public class OgcApiFeaturesAutoConfiguration {

    /**
     * Creates a ModuleStatus bean for OGC API Features.
     */
    @Bean
    ModuleStatus ogcApiFeatureStatus() {
        ModuleStatusImpl moduleStatus = new ModuleStatusImpl();
        moduleStatus.setModule("gs-ogcapi-features");
        moduleStatus.setName("OGC API Features");
        moduleStatus.setAvailable(true);
        moduleStatus.setEnabled(true);
        return moduleStatus;
    }
}
