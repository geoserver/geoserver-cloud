/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.ows.config;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.ows.controller.WMSController;
import org.geoserver.cloud.ows.controller.WMSGetMapReflectorController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = { //
            "jar:gs-wms-.*!/applicationContext.xml", //
            "jar:gs-wfs-.*!/applicationContext.xml#name="
                    + WmsConfiguration.WFS_INCLUDED_BEANS_REGEX //
        })
public class WmsConfiguration {

    static final String WFS_INCLUDED_BEANS_REGEX =
            "^(gml.*OutputFormat"
                    + "|bboxKvpParser"
                    + "|featureIdKvpParser"
                    + "|filter.*_KvpParser"
                    + "|cqlKvpParser"
                    + "|maxFeatureKvpParser"
                    + "|sortByKvpParser"
                    + "|xmlConfiguration.*"
                    + "|gml[1-9]*SchemaBuilder"
                    + "|wfsXsd.*"
                    + "|wfsSqlViewKvpParser"
                    + ").*$";

    public @Bean WMSController webMapServiceController() {
        return new WMSController();
    }

    @ConditionalOnProperty(
            prefix = "geoserver.wms",
            name = "reflector.enabled",
            havingValue = "true",
            matchIfMissing = false)
    public @Bean WMSGetMapReflectorController getMapReflectorController() {
        return new WMSGetMapReflectorController();
    }
}
