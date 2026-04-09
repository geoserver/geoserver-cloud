/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.extension.appschema;

import org.geoserver.spring.config.annotations.ComponentScanStrategy;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class that sets up the App-Schema extension components.
 *
 * @since 3.0.0
 * @see AppSchemaConfiguration_Generated
 * @see AppSchemaConfiguration_Generated.ComponentScannedBeans
 */
@Configuration(proxyBeanMethods = false)
@Import(AppSchemaConfiguration_Generated.class)
@TranspileXmlConfig(
        locations = "jar:gs-app-schema-core-.*!/applicationContext.xml",
        componentScanStrategy = ComponentScanStrategy.GENERATE)
public class AppSchemaConfiguration {}
