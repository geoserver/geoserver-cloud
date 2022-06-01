/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.ows.config;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.ows.controller.WFSController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration(proxyBeanMethods = false)
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = {
            "jar:gs-wfs-.*!/applicationContext.xml#name=.*",
            "jar:gs-flatgeobuf-.*!/applicationContext.xml#name=.*"
        } //
        )
public class WfsConfiguration {

    public @Bean WFSController webFeatureServiceController() {
        return new WFSController();
    }
}
