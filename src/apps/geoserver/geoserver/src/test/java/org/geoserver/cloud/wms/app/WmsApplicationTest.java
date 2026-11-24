/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.wms.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.autoconfigure.extensions.test.ConditionalTestAutoConfiguration;
import org.geoserver.cloud.autoconfigure.gwc.integration.WMSIntegrationAutoConfiguration.ForwardGetMapToGwcAspect;
import org.geoserver.configuration.core.wms.WMSCoreConfiguration;
import org.geoserver.configuration.core.wms.WmsGmlConfiguration;
import org.geoserver.gwc.wms.CachingExtendedCapabilitiesProvider;
import org.geoserver.ows.FlatKvpParser;
import org.geoserver.ows.OWSHandlerMapping;
import org.geoserver.ows.kvp.CQLFilterKvpParser;
import org.geoserver.ows.kvp.SortByKvpParser;
import org.geoserver.ows.kvp.ViewParamsKvpParser;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.wfs.DefaultWebFeatureService;
import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geoserver.wms.GetCapabilities;
import org.geoserver.wms.WMS;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

abstract class WmsApplicationTest {

    protected @Autowired ConfigurableApplicationContext context;

    /** Contributions from {@link WMSCoreConfiguration} */
    @Test
    void testExpectedBeansFromWmsCore() {
        expectBean("wmsURLMapping", OWSHandlerMapping.class);
        expectBean("wms", WMS.class);
        expectBean("wmsExtension", ModuleStatusImpl.class);
        expectBean("wmsGetCapabilities", GetCapabilities.class);
        expectBean("wms", WMS.class);
        expectBean("wmsSqlViewKvpParser", ViewParamsKvpParser.class);
    }

    /** Contributions from {@link WmsGmlConfiguration} */
    @Test
    void testExpectedBeansFromGsWmsGml() {
        expectBean("wmsFilterKvpParser", org.geoserver.wms.kvp.FilterKvpParser.class);
        expectBean("wmsGetFeatureInfoGML2", org.geoserver.wms.featureinfo.GML2FeatureInfoOutputFormat.class);
        expectBean("wmsGetFeatureInfoGML3", org.geoserver.wms.featureinfo.GML3FeatureInfoOutputFormat.class);
        expectBean("wmsGetFeatureInfoXML311", org.geoserver.wms.featureinfo.XML311FeatureInfoOutputFormat.class);

        // wfs-core|wfs1_1
        expectBean("xmlConfiguration-1.1", org.geoserver.wfs.xml.v1_1_0.WFSConfiguration.class);
        expectBean("gml3OutputFormat", GML3OutputFormat.class);
    }

    @Test
    void testExpectedBeansFromGsMain() {
        expectBean("cqlKvpParser", CQLFilterKvpParser.class);
        expectBean("featureIdKvpParser", FlatKvpParser.class);
        expectBean("sortByKvpParser", SortByKvpParser.class);
    }

    @Test
    void testGwcWmsIntegration() {
        expectBean("gwcWMSExtendedCapabilitiesProvider", CachingExtendedCapabilitiesProvider.class);
        expectBean("gwcGetMapAdvise", ForwardGetMapToGwcAspect.class);
    }

    @Test
    void testKmlIntegration() {
        assertThat(context.containsBean("kmlIconsController")).isTrue();
        assertThat(context.containsBean("kmlReflectorController")).isTrue();
    }

    /**
     * Tests the service-specific conditional annotations.
     *
     * <p>Verifies that only the WMS conditional bean is activated in this service, based on the
     * geoserver.service.wms.enabled=true property set in bootstrap.yml. This test relies on the
     * ConditionalTestAutoConfiguration class from the extensions-core test-jar, which contains beans conditionally
     * activated based on each GeoServer service type.
     */
    @Test
    void testServiceConditionalAnnotations() {
        // This should exist in WMS service
        assertThat(context.containsBean("wmsConditionalBean")).isTrue();

        ConditionalTestAutoConfiguration.ConditionalTestBean bean =
                context.getBean("wmsConditionalBean", ConditionalTestAutoConfiguration.ConditionalTestBean.class);
        assertThat(bean.getServiceName()).isEqualTo("WMS");

        // These should not exist in WMS service
        assertThat(context.containsBean("wfsConditionalBean")).isFalse();
        assertThat(context.containsBean("wcsConditionalBean")).isFalse();
        assertThat(context.containsBean("wpsConditionalBean")).isFalse();
        assertThat(context.containsBean("restConditionalBean")).isFalse();
        assertThat(context.containsBean("webUiConditionalBean")).isFalse();
    }

    @Test
    void roleIsolationOnlyWmsEnabled() {
        assertThat(context.getBeansOfType(DefaultWebFeatureService.class)).isEmpty();
    }

    protected void expectBean(String name, Class<?> type) {
        assertThat(context.getBean(name)).isInstanceOf(type);
    }
}
