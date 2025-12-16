/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.kml;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-kml-.*!/applicationContext.xml",
        excludes = {
            "WFSKMLOutputFormat", // not used in WMS
            "kmlURLMapping" // superseded by KMLReflectorController in the AutoConfiguration
        })
@Import(KMLConfiguration_Generated.class)
public class KMLConfiguration {}
