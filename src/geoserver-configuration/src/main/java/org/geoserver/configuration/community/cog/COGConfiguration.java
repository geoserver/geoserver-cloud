/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.community.cog;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to enable the COG (Cloud Optimized GeoTIFF) support as raster
 * data format.
 */
@Configuration
@ImportFilteredResource("jar:gs-cog-.*!/applicationContext.xml#name=" + COGConfiguration.EXCLUDE_WEBUI_BEANS)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class COGConfiguration {

    static final String EXCLUDE_WEBUI_BEANS = "^(?!" + COGWebUIConfiguration.WEBUI_BEAN_NAMES + ").*$";
}
