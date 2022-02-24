/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.wps;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration(proxyBeanMethods = true)
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = {
            // exclude wpsRequestBuilder, DemosAutoConfiguration takes care of it
            "jar:gs-web-wps-.*!/applicationContext.xml#name=^(?!wpsRequestBuilder).*$",
            "jar:gs-wps-.*!/applicationContext.xml",
            "jar:gs-wcs-.*!/applicationContext.xml"
        })
public class WpsConfiguration {}
