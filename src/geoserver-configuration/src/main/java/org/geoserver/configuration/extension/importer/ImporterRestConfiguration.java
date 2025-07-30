package org.geoserver.configuration.extension.importer;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Importer REST API.
 */
@Configuration
@ImportFilteredResource("jar:gs-importer-rest-.*!/applicationContext.xml")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class ImporterRestConfiguration {}
