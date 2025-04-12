/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.backend.core;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.gwc.wmts.WMTSFactoryExtension;
import org.geoserver.gwc.wmts.WMTSXStreamLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wcs.WCSFactoryExtension;
import org.geoserver.wcs.WCSXStreamLoader;
import org.geoserver.wfs.WFSFactoryExtension;
import org.geoserver.wfs.WFSXStreamLoader;
import org.geoserver.wms.WMSFactoryExtension;
import org.geoserver.wms.WMSXStreamLoader;
import org.geoserver.wps.WPSFactoryExtension;
import org.geoserver.wps.WPSXStreamLoader;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Configuration to make sure all {@link XStreamServiceLoader} extensions are loaded regardless of
 * the microservice this starter is used from.
 *
 * @since 1.0
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@Slf4j(topic = "org.geoserver.cloud.config.catalog")
public class XstreamServiceLoadersAutoConfiguration {

    private static final String CONTRIBUTING_MSG = "Automatically contributing {}";

    @ConditionalOnMissingBean(WFSXStreamLoader.class)
    @Bean
    WFSXStreamLoader wfsLoader(GeoServerResourceLoader resourceLoader) {
        return log(new WFSXStreamLoader(resourceLoader));
    }

    @ConditionalOnMissingBean(WFSFactoryExtension.class)
    @Bean
    WFSFactoryExtension wfsFactoryExtension(GeoServerResourceLoader resourceLoader) {
        log(WFSFactoryExtension.class);
        return new WFSFactoryExtension() {}; // constructor is protected!
    }

    @ConditionalOnMissingBean(WMSXStreamLoader.class)
    @Bean
    WMSXStreamLoader wmsLoader(GeoServerResourceLoader resourceLoader) {
        return log(new WMSXStreamLoader(resourceLoader));
    }

    @ConditionalOnMissingBean(WMSFactoryExtension.class)
    @Bean
    WMSFactoryExtension wmsFactoryExtension() {
        return log(new WMSFactoryExtension());
    }

    @ConditionalOnMissingBean(WCSXStreamLoader.class)
    @Bean
    WCSXStreamLoader wcsLoader(GeoServerResourceLoader resourceLoader) {
        return log(new WCSXStreamLoader(resourceLoader));
    }

    @ConditionalOnMissingBean(WCSFactoryExtension.class)
    @Bean
    WCSFactoryExtension wcsFactoryExtension() {
        return log(new WCSFactoryExtension());
    }

    @ConditionalOnMissingBean(WMTSXStreamLoader.class)
    @Bean
    WMTSXStreamLoader wmtsLoader(GeoServerResourceLoader resourceLoader) {
        return log(new WMTSXStreamLoader(resourceLoader));
    }

    @ConditionalOnMissingBean(WMTSFactoryExtension.class)
    @Bean
    WMTSFactoryExtension wmtsFactoryExtension() {
        log(WMTSFactoryExtension.class);
        return new WMTSFactoryExtension() {}; // constructor is protected!
    }

    @ConditionalOnMissingBean(WPSXStreamLoader.class)
    @Bean
    WPSXStreamLoader wpsServiceLoader(GeoServerResourceLoader resourceLoader) {
        return log(new WPSXStreamLoader(resourceLoader));
    }

    @ConditionalOnMissingBean(WPSFactoryExtension.class)
    @Bean
    WPSFactoryExtension wpsFactoryExtension() {
        log(WPSFactoryExtension.class);
        return new WPSFactoryExtension() {}; // constructor is protected!
    }

    private <T> T log(T extension) {
        log(extension.getClass());
        return extension;
    }

    private void log(Class<? extends Object> extensionType) {
        log.info(CONTRIBUTING_MSG, extensionType.getSimpleName());
    }
}
