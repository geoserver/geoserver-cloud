/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.gwc.wmts.WMTSXStreamLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wcs.WCSXStreamLoader;
import org.geoserver.wfs.WFSXStreamLoader;
import org.geoserver.wms.WMSXStreamLoader;
import org.geoserver.wps.WPSXStreamLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to make sure all {@link XStreamServiceLoader} extensions are loaded regardless of
 * the microservice this starter is used from.
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({GeoServerBackendProperties.class, CatalogProperties.class})
@Slf4j(topic = "org.geoserver.cloud.config.catalog")
public class XstreamServiceLoadersConfiguration {

    @ConditionalOnMissingBean(WFSXStreamLoader.class)
    public @Bean WFSXStreamLoader wfsLoader(GeoServerResourceLoader resourceLoader) {
        return log(new WFSXStreamLoader(resourceLoader));
    }

    @ConditionalOnMissingBean(WMSXStreamLoader.class)
    public @Bean WMSXStreamLoader wmsLoader(GeoServerResourceLoader resourceLoader) {
        return log(new WMSXStreamLoader(resourceLoader));
    }

    @ConditionalOnMissingBean(WCSXStreamLoader.class)
    public @Bean WCSXStreamLoader wcsLoader(GeoServerResourceLoader resourceLoader) {
        return log(new WCSXStreamLoader(resourceLoader));
    }

    @ConditionalOnMissingBean(WMTSXStreamLoader.class)
    public @Bean WMTSXStreamLoader wmtsLoader(GeoServerResourceLoader resourceLoader) {
        return log(new WMTSXStreamLoader(resourceLoader));
    }

    @ConditionalOnMissingBean(WPSXStreamLoader.class)
    public @Bean WPSXStreamLoader wpsServiceLoader(GeoServerResourceLoader resourceLoader) {
        return log(new WPSXStreamLoader(resourceLoader));
    }

    private <T extends XStreamServiceLoader<?>> T log(T loader) {
        log.info(
                "Automatically contributing {} service xstream loader",
                loader.getClass().getSimpleName());
        return loader;
    }
}
