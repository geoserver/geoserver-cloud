/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.wms;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = true)
@ImportFilteredResource({
    "jar:gs-web-wms-.*!/applicationContext.xml",
    "jar:gs-wms-.*!/applicationContext.xml#name=" + WmsConfiguration.WMS_BEANS_REGEX,
    "jar:gs-wfs-.*!/applicationContext.xml#name=" + WmsConfiguration.WFS_BEANS_REGEX
})
public class WmsConfiguration {

    static final String WFS_BEANS_REGEX =
            "^(gml.*OutputFormat|bboxKvpParser|xmlConfiguration.*|gml[1-9]*SchemaBuilder|wfsXsd.*|wfsSqlViewKvpParser).*$";

    static final String WMS_BEANS_REGEX =
            """
            ^(?!\
            legendSample\
            |getMapKvpReader\
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
            |wmsJSONLegendOutputFormat\
            ).*$\
            """;
}
