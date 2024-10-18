/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.wfs;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration(proxyBeanMethods = true)
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = { //
            "jar:gs-web-wfs-.*!/applicationContext.xml", //
            "jar:gs-wfs-.*!/applicationContext.xml",
            "jar:gs-flatgeobuf-.*!/applicationContext.xml#name=.*",
            "jar:gs-dxf-core-.*!/applicationContext.xml#name=.*",
            "jar:gs-geopkg-output-.*!/applicationContext.xml#name=.*"
        } //
        )
public class WfsConfiguration {}
