/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.gwc;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration to create a minimal {@link org.geoserver.wms.WebMapService} bean, may it not exist, to be used by
 * GeoWebCache for creating tile images, or by the web-ui service without loading a the full set of WMS beans.
 *
 * <p>For the sake of simplicity, this configuration automatically imports {@link GwcWfsMinimalConfiguration} with the
 * set of WFS dependency beans for the minimal {@code WebMapService} bean.
 *
 * @see WMSCoreMinimalConfiguration_Generated
 * @see GwcWfsMinimalConfiguration
 * @since 2.28.0, previously implemented separately (and duplicated) in {@code gwc-cloud-core} and
 *     {@code gs-cloud-webui}
 */
@Configuration
@TranspileXmlConfig(
        locations = {"jar:gs-wms-core-.*!/applicationContext.xml", "jar:gs-wms1_1-.*!/applicationContext.xml"},
        /**
         * wms beans black-list, note {@code wmsPNGLegendOutputFormat} is not excluded as required by
         * {@link GeoServerTileLayer#getLayerLegendsInfo()}
         */
        excludes = {
            "wmsCapabilitiesXmlReader",
            "getMapXmlReader",
            "sldXmlReader",
            "wmsDescribeLayerXML",
            ".*DescribeLayerResponse",
            "stylesResponse",
            "kmlIconService",
            "wmsURLMapping",
            "wmsXMLTransformerResponse",
            "PDFMap.*",
            "OpenLayers.*",
            "Atom.*",
            "RSSGeoRSSMapProducer",
            ".*SVG.*",
            "metaTileCache",
            ".*LegendGraphicResponse",
            "wmsGIFLegendOutputFormat",
            "wmsJPEGLegendGraphicOutputFormat",
            "wmsJSONLegendOutputFormat"
        })
@Import({GwcWMSMinimalConfiguration_Generated.class, GwcWfsMinimalConfiguration.class})
public class GwcWMSMinimalConfiguration {}
