/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.community.cog;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration to enable the COG customized store panel when the web-ui is
 * present.
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-cog-.*!/applicationContext.xml",
        includes = {"COGGeoTIFFExclusionFilter", "CogGeotiffStorePanel", "CogSettingsPanel"})
@Import(COGWebUIConfiguration_Generated.class)
public class COGWebUIConfiguration {}
