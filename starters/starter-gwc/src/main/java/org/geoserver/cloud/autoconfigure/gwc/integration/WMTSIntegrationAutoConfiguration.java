/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.integration;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnWMTSIntegrationEnabled;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import javax.annotation.PostConstruct;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = true)
@ConditionalOnWMTSIntegrationEnabled
@ImportResource(
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = {"jar:gs-gwc-[0-9]+.*!/geowebcache-geoserver-wmts-integration.xml"})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.integration")
public class WMTSIntegrationAutoConfiguration {

    public @PostConstruct void log() {
        log.info("GeoWebCache WMTS GeoServer integration enabled");
    }
}
