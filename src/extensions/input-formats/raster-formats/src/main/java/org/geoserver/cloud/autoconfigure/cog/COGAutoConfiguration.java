/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.cog;

import org.geoserver.cog.CogSettings;
import org.geoserver.configuration.community.cog.COGConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

/**
 * Auto configuration to enable the COG (Cloud Optimized GeoTIFF) support as
 * raster data format.
 * @see COGConfiguration
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnClass({CogSettings.class})
@Import(COGConfiguration.class)
public class COGAutoConfiguration {}
