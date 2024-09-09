/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import javax.annotation.PostConstruct;

/**
 * @since 1.0
 * @see DefaultTileLayerCatalogConfiguration
 */
@Configuration(proxyBeanMethods = true)
@ImportResource(
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = {GeoServerIntegrationConfiguration.GS_INTEGRATION_INCLUDES})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.core")
public class GeoServerIntegrationConfiguration {

    private static final String EXCLUDED_BEANS =
            """
            ^(?!\
            GeoSeverTileLayerCatalog\
            |gwcCatalogConfiguration\
            |wmsCapabilitiesXmlReader\
            |gwcTransactionListener\
            |gwcWMSExtendedCapabilitiesProvider\
            |gwcInitializer\
            ).*$\
            """;

    static final String GS_INTEGRATION_INCLUDES =
            "jar:gs-gwc-[0-9]+.*!/geowebcache-geoserver-context.xml#name=" + EXCLUDED_BEANS;

    @PostConstruct
    public void log() {
        log.info("GeoWebCache core GeoServer integration enabled");
    }
}
