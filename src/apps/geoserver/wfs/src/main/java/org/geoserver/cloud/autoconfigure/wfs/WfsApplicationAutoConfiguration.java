/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.wfs;

import org.geoserver.cloud.autoconfigure.core.GeoServerWebMvcMainAutoConfiguration;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.boot.autoconfigure.AutoConfiguration;

@AutoConfiguration(after = GeoServerWebMvcMainAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ImportFilteredResource({
    "jar:gs-wfs-core.*!/applicationContext.xml",
    "jar:gs-wfs1_x-.*!/applicationContext.xml",
    "jar:gs-wfs2_x-.*!/applicationContext.xml"
})
public class WfsApplicationAutoConfiguration {}
