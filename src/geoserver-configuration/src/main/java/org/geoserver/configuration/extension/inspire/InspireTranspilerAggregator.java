/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.inspire;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;

/**
 * This is a transpiling aggregator class for the importer extension configurations, hence
 * package private, and generates public configuration classes in this same
 * package:
 *
 * @since 2.27.0.0
 * @see InspireCoreConfiguration
 * @see InspireConfigurationGwc
 * @see InspireConfigurationWcs
 * @see InspireConfigurationWebUI
 * @see InspireConfigurationWfs
 * @see InspireConfigurationWms
 */
@TranspileXmlConfig(
        /*
         * Configuration class that sets up the INSPIRE extension components.
         *
         * This configuration is only active when the INSPIRE extension is enabled
         * through the {@code geoserver.extension.inspire.enabled=true} property.
         */
        locations = "jar:gs-inspire-.*!/applicationContext.xml",
        includes = {"InsiperExtension", "languageCallback", "inspireDirManager"},
        targetClass = "InspireCoreConfiguration",
        publicAccess = true)
@TranspileXmlConfig(
        /*
         * Configuration class that sets up the INSPIRE extension components.
         *
         * This configuration is only active when the INSPIRE extension is enabled
         * through the {@code geoserver.extension.inspire.enabled=true} property.
         */
        locations = "jar:gs-inspire-.*!/applicationContext.xml",
        includes = {"inspireWmtsExtendedCapsProvider", "inspireGridSetLoader"},
        targetClass = "InspireConfigurationGwc",
        publicAccess = true)
@TranspileXmlConfig(
        /*
         * Configuration class that sets up the INSPIRE extension components.
         *
         * This configuration is only active when the INSPIRE extension is enabled
         * through the {@code geoserver.extension.inspire.enabled=true} property.
         */
        locations = "jar:gs-inspire-.*!/applicationContext.xml",
        includes = {"inspireWcsExtendedCapsProvider"},
        targetClass = "InspireConfigurationWcs",
        publicAccess = true)
@TranspileXmlConfig(
        /*
         * Configuration class that sets up the INSPIRE extension components.
         *
         * This configuration is only active when the INSPIRE extension is enabled
         * through the {@code geoserver.extension.inspire.enabled=true} property.
         */
        locations = "jar:gs-inspire-.*!/applicationContext.xml",
        includes = {"inspireWfsExtendedCapsProvider"},
        targetClass = "InspireConfigurationWfs",
        publicAccess = true)
@TranspileXmlConfig(
        /*
         * Configuration class that sets up the INSPIRE extension components.
         *
         * This configuration is only active when the INSPIRE extension is enabled
         * through the {@code geoserver.extension.inspire.enabled=true} property.
         */
        locations = "jar:gs-inspire-.*!/applicationContext.xml",
        includes = {"inspireWmsExtendedCapsProvider"},
        targetClass = "InspireConfigurationWms",
        publicAccess = true)
@TranspileXmlConfig(
        /*
         * Configuration class that sets up the INSPIRE extension components.
         *
         * This configuration is only active when the INSPIRE extension is enabled
         * through the {@code geoserver.extension.inspire.enabled=true} property.
         */
        locations = "jar:gs-inspire-.*!/applicationContext.xml",
        includes = {"inspire.*AdminPanel"},
        targetClass = "InspireConfigurationWebUI",
        publicAccess = true)
class InspireTranspilerAggregator {}
