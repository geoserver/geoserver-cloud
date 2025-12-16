/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.features;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWFS;
import org.geoserver.configuration.extension.ogcapi.core.OgcApiCoreConfiguration;
import org.geoserver.configuration.extension.ogcapi.features.OgcApiFeaturesConfiguration;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Auto-configuration for OGC API Features extension.
 * <p>
 * This auto-configuration class is designed to set up the OGC API Features
 * extension in GeoServer Cloud. It consists of:
 * <ul>
 * <li>Core configuration class that imports the core applicationContext.xml</li>
 * <li>REST configuration class that imports the features applicationContext.xml</li>
 * </ul>
 */
@AutoConfiguration
@EnableWebMvc
@ConditionalOnOgcApiFeatures
@ConditionalOnGeoServerWFS
@EnableConfigurationProperties(OgcApiFeatureConfigProperties.class)
@Import(OgcApiFeaturesConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.ogcapi.features")
public class OgcApiFeaturesAutoConfiguration {

    @PostConstruct
    void log() {
        log.info("OGC API Features extension enabled");
    }

    /**
     * REVISIT: required by {@link APIDispatcher APIDispatcher.initApplicationContext()} through {@code context.getBean()}
     * but not available while processing the transpiled configuration class from {@link OgcApiCoreConfiguration}, though it works when using loading through {@literal applicationContext.xml}
     * <p>
     * Also added to {@link OgcApiFeaturesWebUIAutoConfiguration}
     */
    @Bean
    @ConditionalOnMissingBean(name = "mvcConversionService")
    FormattingConversionService mvcConversionService() {
        return (FormattingConversionService) ApplicationConversionService.getSharedInstance();
    }

    /**
     * REVISIT: required by {@link APIDispatcher APIDispatcher.initApplicationContext()} through {@code context.getBean()}
     * but not available while processing the transpiled configuration class from {@link OgcApiCoreConfiguration}, though it works when using loading through {@literal applicationContext.xml}
     * <p>
     * Also added to {@link OgcApiFeaturesWebUIAutoConfiguration}
     */
    @Bean
    @ConditionalOnMissingBean(name = "mvcValidator")
    Validator mvcValidator(@Qualifier("defaultValidator") Validator defaultValidator) {
        return defaultValidator;
    }

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
