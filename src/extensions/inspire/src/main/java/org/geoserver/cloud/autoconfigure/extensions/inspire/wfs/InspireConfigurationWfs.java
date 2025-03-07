/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire.wfs;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that sets up the INSPIRE extension components.
 *
 * <p>This configuration is only active when the INSPIRE extension is enabled
 * through the {@code geoserver.extension.inspire.enabled=true} property.
 *
 * @since 2.27.0.0
 */
@Configuration
@ConditionalOnClass(name = "org.geoserver.inspire.wfs.WFSExtendedCapabilitiesProvider")
@ImportFilteredResource({
    "jar:gs-inspire-.*!/applicationContext.xml#name=^(inspireWfsExtendedCapsProvider|languageCallback|inspireDirManager).*$"
})
public class InspireConfigurationWfs {}
