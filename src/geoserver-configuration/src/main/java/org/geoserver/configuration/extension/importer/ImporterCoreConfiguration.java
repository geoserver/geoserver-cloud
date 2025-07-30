/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.importer;

import org.geoserver.cloud.config.factory.ImportFilteredResource;

/**
 * Configuration for the GeoServer Importer extension.
 */
@ImportFilteredResource("jar:gs-importer-core-.*!/applicationContext.xml")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class ImporterCoreConfiguration {}
