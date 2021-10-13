/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wms;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource( //
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = { //
        "jar:gs-wms-.*!/applicationContext.xml", //
        "jar:gs-wfs-.*!/applicationContext.xml#name="
                + WmsApplicationConfiguration.WFS_BEANS_REGEX, //
        "jar:gs-css-.*!/applicationContext.xml" //
    }
)
public class WmsApplicationConfiguration {

    static final String WFS_BEANS_REGEX =
            "^(gml.*OutputFormat|bboxKvpParser|xmlConfiguration.*|gml[1-9]*SchemaBuilder|wfsXsd.*|wfsSqlViewKvpParser).*$";

    public @Bean WFSConfiguration wfsConfiguration(GeoServer geoServer) {
        FeatureTypeSchemaBuilder schemaBuilder = new FeatureTypeSchemaBuilder.GML3(geoServer);
        return new WFSConfiguration(geoServer, schemaBuilder, new WFS(schemaBuilder));
    }
}
