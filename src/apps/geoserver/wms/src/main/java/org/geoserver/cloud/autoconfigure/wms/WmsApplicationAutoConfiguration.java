/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.wms;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.autoconfigure.gwc.integration.WMSIntegrationAutoConfiguration;
import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.geoserver.cloud.wms.controller.GetMapReflectorController;
import org.geoserver.cloud.wms.controller.WMSController;
import org.geoserver.config.GeoServer;
import org.geoserver.configuration.core.wms.WMSCoreConfiguration;
import org.geoserver.configuration.core.wms.WmsWfsDependenciesConfiguration;
import org.geoserver.ows.Dispatcher;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @see WMSCoreConfiguration
 * @see WmsWfsDependenciesConfiguration
 */
// auto-configure before GWC's wms-integration to avoid it precluding to load beans from
// jar:gs-wms-.*
@AutoConfiguration(before = WMSIntegrationAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@Import({WMSCoreConfiguration.class, WmsWfsDependenciesConfiguration.class})
public class WmsApplicationAutoConfiguration {

    @Bean
    WFSConfiguration wfsConfiguration(GeoServer geoServer) {
        FeatureTypeSchemaBuilder schemaBuilder = new FeatureTypeSchemaBuilder.GML3(geoServer);
        return new WFSConfiguration(geoServer, schemaBuilder, new WFS(schemaBuilder));
    }

    @Bean
    WMSController webMapServiceController(
            Dispatcher geoserverDispatcher,
            org.geoserver.ows.ClasspathPublisher classPathPublisher,
            VirtualServiceVerifier virtualServiceVerifier) {
        return new WMSController(geoserverDispatcher, classPathPublisher, virtualServiceVerifier);
    }

    @Bean
    VirtualServiceVerifier virtualServiceVerifier(@Qualifier("rawCatalog") Catalog catalog) {
        return new VirtualServiceVerifier(catalog);
    }

    @ConditionalOnProperty(
            prefix = "geoserver.wms",
            name = "reflector.enabled",
            havingValue = "true",
            matchIfMissing = true)
    @Bean
    GetMapReflectorController getMapReflectorController(Dispatcher geoserverDispatcher) {
        return new GetMapReflectorController(geoserverDispatcher);
    }
}
