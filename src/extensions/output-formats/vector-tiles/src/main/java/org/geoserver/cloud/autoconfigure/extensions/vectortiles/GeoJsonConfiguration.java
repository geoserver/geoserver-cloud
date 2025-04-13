/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.vectortiles;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for GeoJSON Vector Tiles format.
 *
 * @since 2.27.0
 */
@ConditionalOnVectorTilesGeoJson
@ImportFilteredResource(
        "jar:gs-vectortiles-.*!/applicationContext.xml#name=(wmsGeoJsonBuilderFactory|wmsGeoJsonMapOutputFormat)")
@Configuration
public class GeoJsonConfiguration {}
