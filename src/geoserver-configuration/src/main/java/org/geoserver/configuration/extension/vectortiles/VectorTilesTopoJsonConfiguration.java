/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.extension.vectortiles;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for TopoJSON Vector Tiles format.
 *
 * @since 2.27.0
 */
@Configuration
@ImportFilteredResource(
        "jar:gs-vectortiles-.*!/applicationContext.xml#name=(wmsTopoJSONBuilderFactory|wmsTopoJSONMapOutputFormat)")
public class VectorTilesTopoJsonConfiguration {}
