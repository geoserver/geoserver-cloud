/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.wms;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.autoconfigure.gwc.integration.WMSIntegrationAutoConfiguration;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.geoserver.cloud.wms.controller.GetMapReflectorController;
import org.geoserver.cloud.wms.controller.WMSController;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geoserver.wms.capabilities.GetCapabilitiesTransformer;
import org.geoserver.wms.capabilities.LegendSample;
import org.geoserver.wms.capabilities.LegendSampleImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

// auto-configure before GWC's wms-integration to avoid it precluding to load beans from
// jar:gs-wms-.*
@AutoConfiguration(before = WMSIntegrationAutoConfiguration.class)
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = { //
            "jar:gs-wms-.*!/applicationContext.xml#name="
                    + WmsApplicationAutoConfiguration.WMS_BEANS_BLACKLIST, //
            "jar:gs-wfs-.*!/applicationContext.xml#name="
                    + WmsApplicationAutoConfiguration.WFS_BEANS_WHITELIST //
        })
public class WmsApplicationAutoConfiguration {

    static final String WFS_BEANS_WHITELIST =
            """
            ^(\
            gml.*OutputFormat\
            |bboxKvpParser\
            |featureIdKvpParser\
            |filter.*_KvpParser\
            |cqlKvpParser\
            |maxFeatureKvpParser\
            |sortByKvpParser\
            |xmlConfiguration.*\
            |gml[1-9]*SchemaBuilder\
            |wfsXsd.*\
            |wfsSqlViewKvpParser\
            ).*$\
            """;

    /** wms beans black-list */
    static final String WMS_BEANS_BLACKLIST =
            """
            ^(?!\
            legendSample\
            ).*$\
            """;

    /**
     * Required by {@link GetCapabilitiesTransformer}, excluded from gs-wms.jar
     *
     * @param catalog using {@code rawCatalog} instead of {@code catalog}, to avoid the local
     *     workspace and secured catalog decorators
     */
    @Bean
    public LegendSample legendSample(
            @Qualifier("rawCatalog") Catalog catalog, GeoServerResourceLoader loader) {
        return new LegendSampleImpl(catalog, loader);
    }

    public @Bean WFSConfiguration wfsConfiguration(GeoServer geoServer) {
        FeatureTypeSchemaBuilder schemaBuilder = new FeatureTypeSchemaBuilder.GML3(geoServer);
        return new WFSConfiguration(geoServer, schemaBuilder, new WFS(schemaBuilder));
    }

    public @Bean WMSController webMapServiceController() {
        return new WMSController();
    }

    public @Bean VirtualServiceVerifier virtualServiceVerifier() {
        return new VirtualServiceVerifier();
    }

    @ConditionalOnProperty(
            prefix = "geoserver.wms",
            name = "reflector.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public @Bean GetMapReflectorController getMapReflectorController() {
        return new GetMapReflectorController();
    }
}
