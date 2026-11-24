/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.wps;

import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWPS;
import org.geoserver.configuration.core.wcs.WCS20Configuration;
import org.geoserver.configuration.core.wcs.WCSCoreConfiguration;
import org.geoserver.configuration.core.wfs.WFSCoreConfiguration;
import org.geoserver.configuration.extension.wps.WPSCoreConfiguration;
import org.geoserver.configuration.extension.wps.WPSSecurityConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @see WPSCoreConfiguration
 * @see WPSSecurityConfiguration
 * @see WCSCoreConfiguration
 * @see WCS20Configuration
 * @see WFSCoreConfiguration
 */
// @AutoConfiguration classes are already proxyBeanMethods = false, matching
// the original WpsApplicationConfiguration(proxyBeanMethods = false)
@AutoConfiguration
@ConditionalOnGeoServerWPS
@Import({
    WPSCoreConfiguration.class,
    WPSSecurityConfiguration.class,
    WCSCoreConfiguration.class,
    WCS20Configuration.class,
    WFSCoreConfiguration.class
})
public class WpsApplicationAutoConfiguration {}
