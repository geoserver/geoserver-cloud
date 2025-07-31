/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.community.cog;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration to enable the COG (Cloud Optimized GeoTIFF) support as raster
 * data format.
 */
@Configuration
@TranspileXmlConfig(
        locations = "jar:gs-cog-.*!/applicationContext.xml",
        // Use org.geoserver.cog as target package because CogEncryptedFieldsProvider is package private
        targetPackage = "org.geoserver.cog",
        // publicAccess must be true for this class to import org.geoserver.cog.COGConfiguration_Generated
        publicAccess = true,
        excludes = {"COGGeoTIFFExclusionFilter", "CogGeotiffStorePanel", "CogSettingsPanel"})
@Import(org.geoserver.cog.COGConfiguration_Generated.class)
public class COGConfiguration {}
