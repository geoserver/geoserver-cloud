/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wps;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = { //
            "jar:gs-wps-.*!/applicationContext.xml", //
            "jar:gs-wfs-.*!/applicationContext.xml", //
            "jar:gs-wcs-.*!/applicationContext.xml", //
            "jar:gs-wcs1_0-.*!/applicationContext.xml", //
            "jar:gs-wcs1_1-.*!/applicationContext.xml", //
            "jar:gs-wcs2_0-.*!/applicationContext.xml" //
            // // REVISIT: wps won't start without the web components! see note in pom.xml
            // "jar:gs-web-core-.*!/applicationContext.xml", //
            // "jar:gs-web-wps-.*!/applicationContext.xml", //
        })
public class WpsApplicationConfiguration {

    @Bean
    VirtualServiceVerifier virtualServiceVerifier(@Qualifier("rawCatalog") Catalog catalog) {
        return new VirtualServiceVerifier(catalog);
    }
}
