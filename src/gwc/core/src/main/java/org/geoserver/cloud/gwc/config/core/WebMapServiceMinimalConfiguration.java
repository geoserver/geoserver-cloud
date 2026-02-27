/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.core;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Configuration to create a minimal {@link WebMapService} bean, may it not exist, to be used by
 * GeoWebCache for creating tile images.
 *
 * @since 1.0
 */
@Configuration
@ImportFilteredResource({
    WebMapServiceMinimalConfiguration.GS_WMS_INCLUDES,
    WebMapServiceMinimalConfiguration.GS_WFS_INCLUDES
})
public class WebMapServiceMinimalConfiguration {

    /**
     * wms beans black-list, note wmsPNGLegendOutputFormat is required by {@link
     * GeoServerTileLayer#getLayerLegendsInfo()}
     */
    private static final String WMS_BEANS_BLACKLIST =
            """
            ^(?!\
            legendSample\
            |getMapKvpReader\
            |wmsCapabilitiesXmlReader\
            |getMapXmlReader\
            |sldXmlReader\
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
            |metaTileCache\
            |wmsClasspathPublisherMapping\
            |.*LegendGraphicResponse\
            |.*LegendOutputFormat\
            ).*$\
            """;

    // wfs beans white-list
    private static final String WFS_BEANS_WHITELIST =
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
            "jar:gs-wms-core-[0-9]+.*!/applicationContext.xml#name=" + WMS_BEANS_BLACKLIST;

    static final String GS_WFS_INCLUDES =
            "jar:gs-wfs-core-[0-9]+.*!/applicationContext.xml#name=" + WFS_BEANS_WHITELIST;

    @Bean
    @DependsOn({"wms"})
    @ConditionalOnMissingBean(GetMapKvpRequestReader.class)
    GetMapKvpRequestReader getMapKvpReader(WMS wms) {
        return new GetMapKvpRequestReader(wms);
    }
}
