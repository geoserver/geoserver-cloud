/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.wms.extension;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.geoserver.inspire.wms.WMSExtendedCapabilitiesProvider")
@ConditionalOnProperty(
        prefix = InspireAutoConfiguration.CONFIG_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@ImportFilteredResource({
    "jar:gs-inspire-.*!/applicationContext.xml#name=^(inspireWmsExtendedCapsProvider|languageCallback|inspireDirManager).*$"
})
public class InspireAutoConfiguration {

    static final String CONFIG_PREFIX = "geoserver.extension.inspire";
}
