/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.extension.wps;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportFilteredResource({
    "jar:gs-wps-.*!/applicationContext.xml",
    "jar:gs-wcs-.*!/applicationContext.xml",
    "jar:gs-wcs1_0-.*!/applicationContext.xml",
    "jar:gs-wcs1_1-.*!/applicationContext.xml",
    "jar:gs-wcs2_0-.*!/applicationContext.xml",
    "jar:gs-wfs-.*!/applicationContext.xml#name=^(?!wfsInsertElementHandler|wfsUpdateElementHandler|wfsDeleteElementHandler|wfsReplaceElementHandler).*$",
    "jar:gs-dxf-core-.*!/applicationContext.xml#name=.*",
    "jar:gs-dxf-wps-.*!/applicationContext.xml#name=.*"
})
public class WPSConfiguration {}
