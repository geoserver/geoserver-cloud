/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.web.demo;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;

/**
 * This is a transpiling aggregator class for web demo configurations, hence
 * package private, and generates public configuration classes in this same
 * package:
 *
 * @see LayerPreviewOpenLayersConfiguration
 * @see LayerPreviewGmlConfiguration
 * @see LayerPreviewKmlConfiguration
 * @see ReprojectionConsoleConfiguration
 * @see SRSListConfiguration
 * @see WCSRequestBuilderConfiguration
 * @see WPSRequestBuilderConfiguration
 */
@TranspileXmlConfig(
        locations = "jar:gs-web-demo-.*!/applicationContext.xml",
        targetClass = "LayerPreviewOpenLayersConfiguration",
        publicAccess = true,
        includes = "openLayersPreview")
@TranspileXmlConfig(
        locations = "jar:gs-web-demo-.*!/applicationContext.xml",
        targetClass = "LayerPreviewGmlConfiguration",
        publicAccess = true,
        includes = "gMLPreview")
@TranspileXmlConfig(
        locations = "jar:gs-web-demo-.*!/applicationContext.xml",
        targetClass = "LayerPreviewKmlConfiguration",
        publicAccess = true,
        includes = "kMLPreview")
@TranspileXmlConfig(
        locations = "jar:gs-web-demo-.*!/applicationContext.xml",
        targetClass = "ReprojectionConsoleConfiguration",
        publicAccess = true,
        includes = "reprojectionConsole")
@TranspileXmlConfig(
        locations = "jar:gs-web-demo-.*!/applicationContext.xml",
        targetClass = "SRSListConfiguration",
        publicAccess = true,
        includes = "srsList")
@TranspileXmlConfig(
        locations = "jar:gs-web-demo-.*!/applicationContext.xml",
        targetClass = "WCSRequestBuilderConfiguration",
        publicAccess = true,
        includes = "wcsRequestBuilder")
@TranspileXmlConfig(
        locations = "jar:gs-web-demo-.*!/applicationContext.xml",
        targetClass = "WPSRequestBuilderConfiguration",
        publicAccess = true,
        includes = "wpsRequestBuilder")
class WebDemoConfigurationTranspileAggregator {}
