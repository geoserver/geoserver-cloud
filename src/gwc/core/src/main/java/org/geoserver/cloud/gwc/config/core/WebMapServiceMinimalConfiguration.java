/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.ImportResource;

/**
 * Configuration to create a minimal {@link WebMapService} bean, may it not exist, to be used by
 * GeoWebCache for creating tile images.
 *
 * @since 1.0
 */
@Configuration
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = { //
            WebMapServiceMinimalConfiguration.GS_WMS_INCLUDES, //
            WebMapServiceMinimalConfiguration.GS_WFS_INCLUDES //
        })
public class WebMapServiceMinimalConfiguration {

    /**
     * wms beans black-list, note wmsPNGLegendOutputFormat is required by {@link
     * GeoServerTileLayer#getLayerLegendsInfo()}
     */
    private static final String WMS_BEANS_REGEX =
            """
            ^(?!\
            getMapKvpReader\
            |wmsCapabilitiesXmlReader\
            |getMapXmlReader\
            |sldXmlReader\
            |wms_1_1_1_GetCapabilitiesResponse\
            |wms_1_3_0_GetCapabilitiesResponse\
            |wmsDescribeLayerXML\
            |.*DescribeLayerResponse\
            |.stylesResponse\
            |.kmlIconService\
            |.wmsURLMapping\
            |.wmsXMLTransformerResponse\
            |.PDFMap.*\
            |OpenLayers.*\
            |Atom.*\
            |RSSGeoRSSMapProducer\
            |.*SVG.*\
            |animateURLMapping\
            |metaTileCache\
            |wmsClasspathPublisherMapping\
            |.*LegendGraphicResponse\
            |wmsGIFLegendOutputFormat\
            |wmsJPEGLegendGraphicOutputFormat\
            |wmsJSONLegendOutputFormat
            ).*$\
            """;

    // wfs beans white-list
    private static final String WFS_BEANS_REGEX =
            """
            ^(\
            gml.*OutputFormat\
            |bboxKvpParser\
            |xmlConfiguration.*\
            |gml[1-9]*SchemaBuilder\
            |wfsXsd.*\
            |wfsSqlViewKvpParser\
            ).*$\
            """;

    static final String GS_WMS_INCLUDES =
            "jar:gs-wms-[0-9]+.*!/applicationContext.xml#name=" + WMS_BEANS_REGEX;

    static final String GS_WFS_INCLUDES =
            "jar:gs-wfs-[0-9]+.*!/applicationContext.xml#name=" + WFS_BEANS_REGEX;

    @Bean
    @Autowired
    @DependsOn({"geoServerLoader", "wms"})
    @ConditionalOnMissingBean(GetMapKvpRequestReader.class)
    GetMapKvpRequestReader getMapKvpReader(WMS wms) {
        return new GetMapKvpRequestReader(wms);
    }
}
