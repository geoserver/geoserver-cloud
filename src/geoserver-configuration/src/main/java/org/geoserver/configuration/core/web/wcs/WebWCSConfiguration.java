/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.web.wcs;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.configuration.core.wcs.WCSConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ImportFilteredResource({
    // exclude wcs request builder, the DemosAutoConfiguration takes care of it
    "jar:gs-web-wcs-.*!/applicationContext.xml#name=^(?!wcsRequestBuilder).*$"
})
@Import(WCSConfiguration.class)
public class WebWCSConfiguration {}
