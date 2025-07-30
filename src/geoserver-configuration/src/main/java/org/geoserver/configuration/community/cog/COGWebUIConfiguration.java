/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.community.cog;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to enable the COG customized store panel when the web-ui is
 * present.
 */
@Configuration
@ImportFilteredResource("jar:gs-cog-.*!/applicationContext.xml#name=" + COGWebUIConfiguration.INCLUDE_WEBUI_BEANS)
public class COGWebUIConfiguration {

    static final String WEBUI_BEAN_NAMES = "COGGeoTIFFExclusionFilter|CogGeotiffStorePanel|CogSettingsPanel";

    static final String INCLUDE_WEBUI_BEANS = "^(" + WEBUI_BEAN_NAMES + ").*$";
}
