/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.wms;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration to include the {@literal gs-wfs} beans required by {@code gs-wms}, useful when not running a full WFS service
 */
@Configuration
@TranspileXmlConfig(
        locations = "jar:gs-wfs-.*!/applicationContext.xml",
        includes = {
            "gml.*OutputFormat",
            "bboxKvpParser",
            "featureIdKvpParser",
            "filter.*_KvpParser",
            "cqlKvpParser",
            "maxFeatureKvpParser",
            "sortByKvpParser",
            "xmlConfiguration.*",
            "gml[1-9]*SchemaBuilder",
            "wfsXsd.*",
            "wfsSqlViewKvpParser"
        })
@Import(WmsWfsDependenciesConfiguration_Generated.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class WmsWfsDependenciesConfiguration {}
