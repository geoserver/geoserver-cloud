/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.extension.importer;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration(proxyBeanMethods = true)
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = { //
            "jar:gs-importer-core-.*!/applicationContext.xml", //
            "jar:gs-importer-web-.*!/applicationContext.xml" //
        } //
        )
public class ImporterConfiguration {}
