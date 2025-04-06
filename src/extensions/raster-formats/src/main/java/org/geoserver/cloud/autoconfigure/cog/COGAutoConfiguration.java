/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.cog;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.cog.CogSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/** Auto configuration to enable the COG (Cloud Optimized GeoTIFF) support as raster data format. */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnClass({CogSettings.class})
@ImportFilteredResource("jar:gs-cog-.*!/applicationContext.xml#name=" + COGAutoConfiguration.EXCLUDE_WEBUI_BEANS)
public class COGAutoConfiguration {

    static final String EXCLUDE_WEBUI_BEANS = "^(?!" + COGWebUIAutoConfiguration.WEBUI_BEAN_NAMES + ").*$";
}
