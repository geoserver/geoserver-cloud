/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.web;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;

/**
 * This is a transpiling aggregator class for core web configurations, hence package private, and generates public
 * configuration classes in this same package:
 *
 * @see WebCoreConfiguration
 * @see WebThemeConfiguration
 * @see WebRestConfiguration
 * @see WebDemoRequestsConfiguration
 * @see WebDemoLayerPreviewConfiguration
 */
@TranspileXmlConfig(
        locations = "jar:gs-web-core-.*!/applicationContext.xml",
        targetClass = "WebCoreConfiguration",
        publicAccess = true,
        // The LogPage of this menu entry shows the log file, and the services log to the console only
        excludes = {"logsPage"})
@TranspileXmlConfig(
        locations = "jar:gs-theme-.*!/applicationContext.xml",
        targetClass = "WebThemeConfiguration",
        publicAccess = true)
@TranspileXmlConfig(
        locations = "jar:gs-web-rest-.*!/applicationContext.xml",
        targetClass = "WebRestConfiguration",
        publicAccess = true)
@TranspileXmlConfig(
        locations = "jar:gs-web-demo-.*!/applicationContext.xml",
        targetClass = "WebDemoRequestsConfiguration",
        publicAccess = true,
        includes = "demoRequests")
@TranspileXmlConfig(
        locations = "jar:gs-web-demo-.*!/applicationContext.xml",
        targetClass = "WebDemoLayerPreviewConfiguration",
        publicAccess = true,
        includes = "layerListDemo")
class WebCoreConfigurationTranspileAggregator {}
