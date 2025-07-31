/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.importer;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Import;

/**
 * Configuration for the GeoServer Importer extension.
 */
@TranspileXmlConfig(locations = "jar:gs-importer-core-.*!/applicationContext.xml")
@Import(ImporterCoreConfiguration_Generated.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class ImporterCoreConfiguration {}
