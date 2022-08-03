/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.wms;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.geoserver.cloud.wms.controller.GetMapReflectorController;
import org.geoserver.cloud.wms.controller.WMSController;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
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
                    + WmsApplicationAutoConfiguration.WFS_INCLUDED_BEANS_REGEX //
        })
public class WmsApplicationAutoConfiguration {

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

    public @Bean WFSConfiguration wfsConfiguration(GeoServer geoServer) {
        FeatureTypeSchemaBuilder schemaBuilder = new FeatureTypeSchemaBuilder.GML3(geoServer);
        return new WFSConfiguration(geoServer, schemaBuilder, new WFS(schemaBuilder));
    }

    public @Bean WMSController webMapServiceController() {
        return new WMSController();
    }

    public @Bean VirtualServiceVerifier virtualServiceVerifier() {
        return new VirtualServiceVerifier();
    }

    @ConditionalOnProperty(
            prefix = "geoserver.wms",
            name = "reflector.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public @Bean GetMapReflectorController getMapReflectorController() {
        return new GetMapReflectorController();
    }
}
