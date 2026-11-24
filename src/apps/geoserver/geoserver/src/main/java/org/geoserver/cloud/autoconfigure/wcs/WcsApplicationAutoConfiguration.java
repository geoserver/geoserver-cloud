/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.wcs;

import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWCS;
import org.geoserver.configuration.core.wcs.WCS20Configuration;
import org.geoserver.configuration.core.wcs.WCSCoreConfiguration;
import org.geoserver.configuration.extension.wcs.WCS10Configuration;
import org.geoserver.configuration.extension.wcs.WCS11Configuration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @see WCSCoreConfiguration
 * @see WCS10Configuration
 * @see WCS11Configuration
 * @see WCS20Configuration
 */
@AutoConfiguration
@ConditionalOnGeoServerWCS
@Import({WCSCoreConfiguration.class, WCS10Configuration.class, WCS11Configuration.class, WCS20Configuration.class})
public class WcsApplicationAutoConfiguration {}
