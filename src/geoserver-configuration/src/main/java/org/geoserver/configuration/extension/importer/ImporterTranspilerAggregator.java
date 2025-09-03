/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.importer;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;

/**
 * This is a transpiling aggregator class for the importer extension configurations, hence
 * package private, and generates public configuration classes in this same
 * package:
 *
 * @see ImporterCoreConfiguration
 * @see ImporterRestConfiguration
 * @see ImporterWebUIConfiguration
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-importer-core-.*!/applicationContext.xml",
        targetClass = "ImporterCoreConfiguration",
        publicAccess = true)
@TranspileXmlConfig(
        locations = "jar:gs-importer-rest-.*!/applicationContext.xml",
        targetClass = "ImporterRestConfiguration",
        publicAccess = true)
@TranspileXmlConfig(
        locations = "jar:gs-importer-web-.*!/applicationContext.xml",
        targetClass = "ImporterWebUIConfiguration",
        publicAccess = true)
class ImporterTranspilerAggregator {}
