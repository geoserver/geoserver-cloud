/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.wms;

import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Service;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.geoserver.wms.WMSServiceExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration to create a minimal {@link WebMapService} bean, may it not
 * exist, to be used by GeoWebCache for creating tile images, or by the web-ui
 * service without loading a the full set of WMS beans.
 * <p>
 * For the sake of simplicity, this configuration automatically imports
 * {@link WmsWfsDependenciesConfiguration} with the set of WFS dependency beans
 * for the minimal {@code WebMapService} bean.
 *
 * @since 2.28.0, previously implemented separately (and duplicated) in
 *        {@code gwc-cloud-core} and {@code gs-cloud-webui}
 *
 * @see WMSCoreMinimalConfiguration_Generated
 * @see WmsWfsDependenciesConfiguration
 */
@Configuration
@TranspileXmlConfig(
        locations = "jar:gs-wms-[0-9]+.*!/applicationContext.xml",
        /**
         * wms beans black-list, note
         * {@code wmsPNGLegendOutputFormat} is not excluded as required by {@link
         * GeoServerTileLayer#getLayerLegendsInfo()}
         */
        excludes = {
            "legendSample",
            "wmsCapabilitiesXmlReader",
            "getMapXmlReader",
            "sldXmlReader",
            "wms_1_1_1_GetCapabilitiesResponse",
            "wms_1_3_0_GetCapabilitiesResponse",
            "wmsDescribeLayerXML",
            ".*DescribeLayerResponse",
            ".stylesResponse",
            ".kmlIconService",
            ".wmsURLMapping",
            ".wmsXMLTransformerResponse",
            ".PDFMap.*",
            "OpenLayers.*",
            "Atom.*",
            "RSSGeoRSSMapProducer",
            ".*SVG.*",
            "animateURLMapping",
            "metaTileCache",
            "wmsClasspathPublisherMapping",
            ".*LegendGraphicResponse",
            "wmsGIFLegendOutputFormat",
            "wmsJPEGLegendGraphicOutputFormat",
            "wmsJSONLegendOutputFormat",
            "wmsExceptionHandler"
        })
@Import({WMSCoreMinimalConfiguration_Generated.class, WmsWfsDependenciesConfiguration.class})
public class WMSCoreMinimalConfiguration {
    /**
     * Conditionally return a {@link WMSServiceExceptionHandler} in case it doesn't exist. It's excluded from {@literal applicationContext.xml}
     * because the wms-service app overrides it
     */
    @Bean
    @ConditionalOnMissingBean
    WMSServiceExceptionHandler wmsExceptionHandler(
            @SuppressWarnings("java:S6830") @Qualifier("wms-1_1_1-ServiceDescriptor") Service wms11,
            @SuppressWarnings("java:S6830") @Qualifier("wms-1_3_0-ServiceDescriptor") Service wms13,
            GeoServer geoServer) {
        return new WMSServiceExceptionHandler(List.of(wms11, wms13), geoServer);
    }
}
