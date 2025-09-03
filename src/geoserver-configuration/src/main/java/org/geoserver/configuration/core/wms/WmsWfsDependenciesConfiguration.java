/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.wms;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration to include the {@literal gs-wfs} beans required by
 * {@code gs-wms}, useful when not running a full WFS service
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-wfs-.*!/applicationContext.xml",
        includes = WmsWfsDependenciesConfiguration.WFS_CORE_INCLUDES_REGEX)
@Import(WmsWfsDependenciesConfiguration_Generated.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class WmsWfsDependenciesConfiguration {

    /**
     * These are wfs core specific filters that required a careful evaluation of
     * what to include to support a minimal WMS without the WFS service running
     * (i.e. without including all {@code gs-wfs} beans in the application context.
     */
    static final String WFS_CORE_INCLUDES_REGEX =
            "^(gml.*OutputFormat|bboxKvpParser|xmlConfiguration.*|gml[1-9]*SchemaBuilder|wfsXsd.*|wfsSqlViewKvpParser).*$";
}
