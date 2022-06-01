/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.ows.config;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.ows.controller.WCSController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = { //
            "jar:gs-wcs-.*!/applicationContext.xml", //
            "jar:gs-wcs1_0-.*!/applicationContext.xml", //
            "jar:gs-wcs1_1-.*!/applicationContext.xml", //
            "jar:gs-wcs2_0-.*!/applicationContext.xml" //
        })
public class WcsConfiguration {

    public @Bean WCSController webCoverageServiceController() {
        return new WCSController();
    }
}
