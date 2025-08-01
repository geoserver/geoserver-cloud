/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.wms;

import lombok.Getter;
import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.configuration.core.web.wms.WebWMSConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @see WebWMSConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.geoserver.wms.web.WMSAdminPage")
@ConditionalOnProperty( // enabled by default
        name = "geoserver.web-ui.wms.enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import({WebWMSConfiguration.class})
@ImportFilteredResource({
    "jar:gs-wms-.*!/applicationContext.xml#name=" + WebWmsAutoConfiguration.WMS_CORE_EXCLUDES_REGEX,
    "jar:gs-wfs-.*!/applicationContext.xml#name=" + WebWmsAutoConfiguration.WFS_CORE_INCLUDES_REGEX
})
public class WebWmsAutoConfiguration extends AbstractWebUIAutoConfiguration {

    static final String WFS_CORE_INCLUDES_REGEX =
            "^(gml.*OutputFormat|bboxKvpParser|xmlConfiguration.*|gml[1-9]*SchemaBuilder|wfsXsd.*|wfsSqlViewKvpParser).*$";

    static final String WMS_CORE_EXCLUDES_REGEX =
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

    @Getter
    private final String configPrefix = "geoserver.web-ui.wms";
}
