/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.kml;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

/**
 * @since 1.0
 */
@Configuration
@ImportFilteredResource("jar:gs-kml-.*!/applicationContext.xml#name=^(?!WFSKMLOutputFormat|kmlURLMapping).*$")
public class KMLConfiguration {}
