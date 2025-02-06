/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wms.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.autoconfigure.gwc.integration.WMSIntegrationAutoConfiguration.ForwardGetMapToGwcAspect;
import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.geoserver.cloud.wms.controller.GetMapReflectorController;
import org.geoserver.cloud.wms.controller.WMSController;
import org.geoserver.cloud.wms.controller.kml.KMLIconsController;
import org.geoserver.cloud.wms.controller.kml.KMLReflectorController;
import org.geoserver.gwc.wms.CachingExtendedCapabilitiesProvider;
import org.geoserver.ows.FlatKvpParser;
import org.geoserver.ows.kvp.CQLFilterKvpParser;
import org.geoserver.ows.kvp.SortByKvpParser;
import org.geoserver.ows.kvp.ViewParamsKvpParser;
import org.geoserver.ows.util.NumericKvpParser;
import org.geoserver.wfs.kvp.BBoxKvpParser;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

abstract class WmsApplicationTest {

    protected @Autowired ConfigurableApplicationContext context;

    @Test
    void testExpectedBeansFromWmsApplicationAutoConfiguration() {
        expectBean("wfsConfiguration", WFSConfiguration.class);
        expectBean("webMapServiceController", WMSController.class);
        expectBean("virtualServiceVerifier", VirtualServiceVerifier.class);
        expectBean("getMapReflectorController", GetMapReflectorController.class);
        expectBean("wms_1_1_1_GetCapabilitiesResponse", org.geoserver.wms.capabilities.GetCapabilitiesResponse.class);
        expectBean("wms_1_1_1_GetCapabilitiesResponse", org.geoserver.wms.capabilities.GetCapabilitiesResponse.class);
        expectBean("wmsExceptionHandler", StatusCodeWmsExceptionHandler.class);
    }

    @Test
    void testExpectedBeansFromGsWfsJarFile() {
        expectBean("bboxKvpParser", BBoxKvpParser.class);
        expectBean("featureIdKvpParser", FlatKvpParser.class);
        expectBean("cqlKvpParser", CQLFilterKvpParser.class);
        expectBean("maxFeatureKvpParser", NumericKvpParser.class);
        expectBean("sortByKvpParser", SortByKvpParser.class);
        expectBean("wfsSqlViewKvpParser", ViewParamsKvpParser.class);

        expectBean("wfsXsd-1.0", org.geoserver.wfs.xml.v1_0_0.WFS.class);
        expectBean("wfsXmlConfiguration-1.0", org.geoserver.wfs.xml.v1_0_0.WFSConfiguration.class);

        expectBean("wfsXsd-1.1", org.geoserver.wfs.xml.v1_1_0.WFS.class);
        expectBean("wfsXmlConfiguration-1.1", org.geoserver.wfs.xml.v1_1_0.WFSConfiguration.class);

        expectBean("wfsXsd-1.0", org.geoserver.wfs.xml.v1_0_0.WFS.class);
        expectBean("wfsXmlConfiguration-1.0", org.geoserver.wfs.xml.v1_0_0.WFSConfiguration.class);

        expectBean("filter1_0_0_KvpParser", org.geoserver.wfs.kvp.Filter_1_0_0_KvpParser.class);
        expectBean("filter1_1_0_KvpParser", org.geoserver.wfs.kvp.Filter_1_1_0_KvpParser.class);
        expectBean("filter2_0_0_KvpParser", org.geoserver.wfs.kvp.Filter_2_0_0_KvpParser.class);

        expectBean("gml2SchemaBuilder", org.geoserver.wfs.xml.FeatureTypeSchemaBuilder.GML2.class);
        expectBean("gml3SchemaBuilder", org.geoserver.wfs.xml.FeatureTypeSchemaBuilder.GML3.class);

        expectBean("gml2OutputFormat", org.geoserver.wfs.xml.GML2OutputFormat.class);
        expectBean("gml3OutputFormat", org.geoserver.wfs.xml.GML3OutputFormat.class);
        expectBean("gml32OutputFormat", org.geoserver.wfs.xml.GML32OutputFormat.class);
    }

    @Test
    void testGwcWmsIntegration() {
        expectBean("gwcWMSExtendedCapabilitiesProvider", CachingExtendedCapabilitiesProvider.class);
        expectBean("gwcGetMapAdvise", ForwardGetMapToGwcAspect.class);
    }

    @Test
    void testKmlIntegration() {
        expectBean("kmlIconsController", KMLIconsController.class);
        expectBean("kmlReflectorController", KMLReflectorController.class);
    }

    protected void expectBean(String name, Class<?> type) {
        assertThat(context.getBean(name)).isInstanceOf(type);
    }
}
