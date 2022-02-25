/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.service;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheConfigurationProperties;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.gwc.web.mgmaps.MGMapsController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import javax.annotation.PostConstruct;

/**
 * @since 1.0
 */
@Configuration
@ConditionalOnProperty(
        name = GeoWebCacheConfigurationProperties.SERVICE_MGMAPS_ENABLED,
        havingValue = "true",
        matchIfMissing = false)
@ComponentScan(basePackageClasses = MGMapsController.class)
@ImportResource(
        reader = FilteringXmlBeanDefinitionReader.class,
        locations =
                "jar:gs-gwc-[0-9]+.*!/geowebcache-gmaps-context.xml#name=gwcServiceMGMapsTarget")
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.service")
public class MGMapsAutoConfiguration {

    public @PostConstruct void log() {
        log.info("{} enabled", GeoWebCacheConfigurationProperties.SERVICE_MGMAPS_ENABLED);
    }
}
