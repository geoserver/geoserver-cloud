/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.services;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geowebcache.service.gmaps.GMapsConverter;
import org.gwc.web.gmaps.GoogleMapsController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(GMapsConverter.class)
@ComponentScan(basePackageClasses = GoogleMapsController.class)
@ImportFilteredResource("jar:gs-gwc-[0-9]+.*!/geowebcache-gmaps-context.xml#name=gwcServiceGMapsTarget")
public class GoogleMapsConfiguration {}
