/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.wfs;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = true)
@ImportFilteredResource({
    "jar:gs-web-wfs-.*!/applicationContext.xml",
    "jar:gs-wfs-.*!/applicationContext.xml",
    "jar:gs-dxf-core-.*!/applicationContext.xml#name=.*"
    // FlatGeobuf moved to output-formats/flatgeobuf extension
})
public class WfsConfiguration {}
