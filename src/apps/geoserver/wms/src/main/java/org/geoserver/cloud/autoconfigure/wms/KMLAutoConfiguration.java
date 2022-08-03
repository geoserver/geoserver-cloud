/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.wms;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.wms.controller.kml.KMLIconsController;
import org.geoserver.cloud.wms.controller.kml.KMLReflectorController;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "geoserver.wms.kml.enabled",
        havingValue = "true",
        matchIfMissing = true)
@ConditionalOnClass(MBStyleHandler.class)
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = {
            "jar:gs-kml-.*!/applicationContext.xml#name=^(?!WFSKMLOutputFormat|kmlURLMapping).*$"
        })
public class KMLAutoConfiguration {

    public @Bean KMLIconsController kmlIconsController() {
        return new KMLIconsController();
    }

    public @Bean KMLReflectorController kmlReflectorController() {
        return new KMLReflectorController();
    }
}
