/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.wps;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = true)
@ImportFilteredResource({
    // exclude wpsRequestBuilder, DemosAutoConfiguration takes care of it
    "jar:gs-web-wps-.*!/applicationContext.xml#name=^(?!wpsRequestBuilder).*$",
    "jar:gs-wps-.*!/applicationContext.xml",
    "jar:gs-wcs-.*!/applicationContext.xml",
    "jar:gs-dxf-core-.*!/applicationContext.xml#name=.*",
    "jar:gs-dxf-wps-.*!/applicationContext.xml#name=.*"
})
public class WpsConfiguration {}
