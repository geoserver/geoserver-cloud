package org.geoserver.configuration.extension.importer;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Importer Web UI components.
 */
@Configuration
@ImportFilteredResource("jar:gs-importer-web-.*!/applicationContext.xml")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class ImporterWebUIConfiguration {}
