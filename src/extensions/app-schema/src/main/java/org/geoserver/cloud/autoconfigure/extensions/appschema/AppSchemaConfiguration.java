/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.appschema;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that sets up the App-Schema extension components.
 *
 * <p>This configuration is only active when the App-Schema extension is enabled
 * through the {@code geoserver.extension.appschema.enabled=true} property.
 *
 * <p>It scans the {@code org.geoserver.complex} package to register all App-Schema
 * related components in the Spring application context.
 *
 * @since 2.27.0.0
 */
@Configuration
@ConditionalOnAppSchema
@ComponentScan(basePackages = "org.geoserver.complex")
public class AppSchemaConfiguration {}
