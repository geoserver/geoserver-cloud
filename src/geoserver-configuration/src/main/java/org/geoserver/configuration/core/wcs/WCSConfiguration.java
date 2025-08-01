/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.wcs;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ImportFilteredResource({ //
    "jar:gs-wcs-.*!/applicationContext.xml", //
    "jar:gs-wcs1_0-.*!/applicationContext.xml", //
    "jar:gs-wcs1_1-.*!/applicationContext.xml", //
    "jar:gs-wcs2_0-.*!/applicationContext.xml" //
})
@Import({WCSCoreConfiguration.class, WCS1_0_Configuration.class, WCS1_1Configuration.class, WCS2_0Configuration.class})
public class WCSConfiguration {}
