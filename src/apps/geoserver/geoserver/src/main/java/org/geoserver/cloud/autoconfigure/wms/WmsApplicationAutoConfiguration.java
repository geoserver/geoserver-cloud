/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.wms;

import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWMS;
import org.geoserver.cloud.autoconfigure.gwc.integration.WMSIntegrationAutoConfiguration;
import org.geoserver.config.GeoServer;
import org.geoserver.configuration.core.wms.WMS11Configuration;
import org.geoserver.configuration.core.wms.WMS13Configuration;
import org.geoserver.configuration.core.wms.WMSCoreConfiguration;
import org.geoserver.configuration.core.wms.WmsGmlConfiguration;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @see WMSCoreConfiguration
 * @see WMS11Configuration
 * @see WMS13Configuration
 * @see WmsGmlConfiguration
 */
// auto-configure before GWC's wms-integration to avoid it precluding to load beans from jar:gs-wms-.*
@AutoConfiguration(before = WMSIntegrationAutoConfiguration.class)
@ConditionalOnGeoServerWMS
@Import({WMSCoreConfiguration.class, WMS11Configuration.class, WMS13Configuration.class, WmsGmlConfiguration.class})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class WmsApplicationAutoConfiguration {

    @Bean(name = "xmlConfiguration-1.1")
    @ConditionalOnMissingBean
    @SuppressWarnings("java:S6830") // unconventional bean name
    WFSConfiguration wfsConfiguration(GeoServer geoServer) {
        FeatureTypeSchemaBuilder schemaBuilder = new FeatureTypeSchemaBuilder.GML3(geoServer);
        org.geoserver.wfs.xml.v1_1_0.WFS wfs = new org.geoserver.wfs.xml.v1_1_0.WFS(schemaBuilder);
        return new WFSConfiguration(geoServer, schemaBuilder, wfs);
    }

    @Bean(name = "gml3OutputFormat")
    @ConditionalOnMissingBean
    @SuppressWarnings("java:S6830") // unconventional bean name
    GML3OutputFormat gml3OutputFormat(
            GeoServer geoserver, @Qualifier("xmlConfiguration-1.1") WFSConfiguration configuration) {
        return new GML3OutputFormat(geoserver, configuration);
    }
}
