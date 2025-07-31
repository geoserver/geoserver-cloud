package org.geoserver.configuration.extension.importer;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for the Importer REST API.
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(locations = "jar:gs-importer-rest-.*!/applicationContext.xml")
@Import(ImporterRestConfiguration_Generated.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class ImporterRestConfiguration {}
