/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.community.cog;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration to enable the COG (Cloud Optimized GeoTIFF) support as raster data format.
 *
 * <p>Excludes webui components.
 *
 * @see COGConfiguration_Generated
 * @see COGWebUIConfiguration
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-cog-.*!/applicationContext.xml",
        excludes = {
            "COGGeoTIFFExclusionFilter",
            "CogGeotiffStorePanel",
            "CogSettingsPanel",
            // package-private class, excluded until geoserver/geoserver#10133 is in the GeoServer build
            "cogEncryptedFieldsProvider"
        })
@Import(COGConfiguration_Generated.class)
public class COGConfiguration {}
