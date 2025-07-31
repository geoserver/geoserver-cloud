/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.features;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.configuration.extension.ogcapi.core.OgcApiCoreConfiguration;
import org.geoserver.configuration.extension.ogcapi.features.OgcApiFeaturesWebUIConfiguration;
import org.geoserver.ogcapi.APIDispatcher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.Validator;

/**
 * @see OgcApiFeaturesWebUIConfiguration
 */
@AutoConfiguration
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

    /**
     * REVISIT: required by {@link APIDispatcher APIDispatcher.initApplicationContext()} through {@code context.getBean()}
     * but not available while processing the transpiled configuration class from {@link OgcApiCoreConfiguration}, though it works when using loading through {@literal applicationContext.xml}
     * <p>
     * Also added to {@link OgcApiFeaturesAutoConfiguration}
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
     * Also added to {@link OgcApiFeaturesAutoConfiguration}
     */
    @Bean
    @ConditionalOnMissingBean(name = "mvcValidator")
    Validator mvcValidator(@Qualifier("defaultValidator") Validator defaultValidator) {
        return defaultValidator;
    }
}
