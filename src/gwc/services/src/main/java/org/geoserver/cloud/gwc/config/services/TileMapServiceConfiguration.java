/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.config.services;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geowebcache.service.tms.TMSService;
import org.gwc.web.tms.TMSController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(TMSService.class)
@ComponentScan(basePackageClasses = TMSController.class)
@ImportResource(
        reader = FilteringXmlBeanDefinitionReader.class,
        locations = "jar:gs-gwc-[0-9]+.*!/geowebcache-tmsservice-context.xml")
public class TileMapServiceConfiguration {}
