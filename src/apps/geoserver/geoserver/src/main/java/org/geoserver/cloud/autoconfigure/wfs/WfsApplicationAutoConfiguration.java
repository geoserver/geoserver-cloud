/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.wfs;

import org.geoserver.cloud.autoconfigure.core.GeoServerMainAutoConfiguration;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWFS;
import org.geoserver.configuration.core.wfs.WFS1xConfiguration;
import org.geoserver.configuration.core.wfs.WFS2xConfiguration;
import org.geoserver.configuration.core.wfs.WFSCoreConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @see WFSCoreConfiguration
 * @see WFS1xConfiguration
 * @see WFS2xConfiguration
 */
@AutoConfiguration(after = GeoServerMainAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnGeoServerWFS
@Import({WFSCoreConfiguration.class, WFS1xConfiguration.class, WFS2xConfiguration.class})
public class WfsApplicationAutoConfiguration {}
