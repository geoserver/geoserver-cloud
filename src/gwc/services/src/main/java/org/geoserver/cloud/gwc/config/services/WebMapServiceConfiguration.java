/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.services;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geowebcache.service.wms.WMSService;
import org.gwc.web.wms.WMSController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WMSService.class)
@ComponentScan(basePackageClasses = WMSController.class)
@ImportFilteredResource("jar:gs-gwc-[0-9]+.*!/geowebcache-wmsservice-context.xml")
public class WebMapServiceConfiguration {}
