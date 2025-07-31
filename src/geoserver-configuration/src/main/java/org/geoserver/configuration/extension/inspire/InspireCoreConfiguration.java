/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.inspire;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class that sets up the INSPIRE extension components.
 *
 * <p>
 * This configuration is only active when the INSPIRE extension is enabled
 * through the {@code geoserver.extension.inspire.enabled=true} property.
 *
 * @since 2.27.0.0
 */
@Configuration
@TranspileXmlConfig(
        locations = "jar:gs-inspire-.*!/applicationContext.xml",
        includes = {"InsiperExtension", "languageCallback", "inspireDirManager"})
@Import(InspireCoreConfiguration_Generated.class)
public class InspireCoreConfiguration {}
