/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.extension.vectortiles;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for GeoJSON Vector Tiles format.
 *
 * @since 2.27.0
 */
@Configuration
@TranspileXmlConfig(
        locations = "jar:gs-vectortiles-.*!/applicationContext.xml",
        includes = {"wmsGeoJsonBuilderFactory", "wmsGeoJsonMapOutputFormat"})
@Import(VectorTilesGeoJsonConfiguration_Generated.class)
public class VectorTilesGeoJsonConfiguration {}
