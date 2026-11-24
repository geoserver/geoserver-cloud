/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.features;

import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWFS;
import org.geoserver.configuration.extension.ogcapi.features.OgcApiFeaturesConfiguration;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for OGC API Features extension.
 *
 * <p>This auto-configuration class is designed to set up the OGC API Features extension in GeoServer Cloud. It consists
 * of:
 *
 * <ul>
 *   <li>Core configuration class that imports the core applicationContext.xml
 *   <li>REST configuration class that imports the features applicationContext.xml
 * </ul>
 *
 * <p>Ordered after Spring Boot's Web MVC auto-configuration, which is what decides who owns Spring MVC in this context.
 * The OGC API contributes {@code APIConfigurationSupport}, a {@code WebMvcConfigurationSupport} subclass registered as
 * a plain bean rather than as a configuration class. Reaching the context before Spring Boot's
 * {@code @ConditionalOnMissingBean(WebMvcConfigurationSupport.class)} is evaluated would make Boot step aside for a
 * bean that configures nothing, leaving the context without a Spring MVC owner at all.
 *
 * <p>The ordering is by name because {@code spring-boot-webmvc} is not a dependency of this module.
 */
@AutoConfiguration(afterName = "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration")
@EnableConfigurationProperties(OgcApiFeatureConfigProperties.class)
@ConditionalOnOgcApiFeatures
@ConditionalOnGeoServerWFS
@Import({OgcApiFeaturesConfiguration.class, OgcApiFeaturesWebUIAutoConfiguration.class})
public class OgcApiFeaturesAutoConfiguration {

    /** Creates a ModuleStatus bean for OGC API Features. */
    @Bean
    ModuleStatus ogcApiFeatureStatus() {
        ModuleStatusImpl moduleStatus = new ModuleStatusImpl();
        moduleStatus.setCategory(ModuleStatus.Category.EXTENSION);
        moduleStatus.setModule("gs-ogcapi-features");
        moduleStatus.setName("OGC API Features");
        moduleStatus.setAvailable(true);
        moduleStatus.setEnabled(true);
        return moduleStatus;
    }
}
