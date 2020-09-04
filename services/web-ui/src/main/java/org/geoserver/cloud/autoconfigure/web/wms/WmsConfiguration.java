/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.wms;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration(proxyBeanMethods = true)
@ImportResource( //
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = {
        "jar:gs-web-wms-.*!/applicationContext.xml", //
        "jar:gs-wms-.*!/applicationContext.xml", //
        "jar:gs-wfs-.*!/applicationContext.xml#name=" + WmsConfiguration.WFS_BEANS_REGEX
    }
)
public class WmsConfiguration {

    static final String WFS_BEANS_REGEX =
            "^(gml.*OutputFormat|bboxKvpParser|xmlConfiguration.*|gml[1-9]*SchemaBuilder|wfsXsd.*|wfsSqlViewKvpParser).*$";
}
