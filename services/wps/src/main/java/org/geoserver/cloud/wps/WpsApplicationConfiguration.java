/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wps;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource( //
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = { //
        "jar:gs-wps-.*!/applicationContext.xml" // , //
        // // REVISIT: wps won't start without the web components! see note in pom.xml
        // "jar:gs-web-core-.*!/applicationContext.xml", //
        // "jar:gs-web-wps-.*!/applicationContext.xml", //
    }
)
public class WpsApplicationConfiguration {}
