/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.wfs.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.geoserver.cloud.autoconfigure.extensions.test.ConditionalTestAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.xmlunit.assertj3.XmlAssert;

@SpringBootTest(classes = WfsApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
abstract class WfsApplicationTest {

    protected TestRestTemplate restTemplate = new TestRestTemplate("admin", "geoserver");

    @Autowired
    protected ApplicationContext context;

    @Test
    void owsGetCapabilitiesSmokeTest(@LocalServerPort int servicePort) {
        String url = "http://localhost:%d/ows?SERVICE=WFS&REQUEST=GETCAPABILITIES&VERSION=1.1.0".formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);
        Map<String, String> nscontext = Map.of("wfs", "http://www.opengis.net/wfs");
        XmlAssert.assertThat(caps).withNamespaceContext(nscontext).hasXPath("/wfs:WFS_Capabilities");
    }

    @Test
    void wfsGetCapabilitiesSmokeTest(@LocalServerPort int servicePort) {
        String url = "http://localhost:%d/wfs?SERVICE=WFS&REQUEST=GETCAPABILITIES&VERSION=1.1.0".formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);
        Map<String, String> nscontext = Map.of("wfs", "http://www.opengis.net/wfs");
        XmlAssert.assertThat(caps).withNamespaceContext(nscontext).hasXPath("/wfs:WFS_Capabilities");
    }

    /**
     * Tests the service-specific conditional annotations.
     *
     * <p>
     * Verifies that only the WFS conditional bean is activated in this service,
     * based on the geoserver.service.wfs.enabled=true property set in bootstrap.yml.
     * This test relies on the ConditionalTestAutoConfiguration class from the
     * extensions-core test-jar, which contains beans conditionally activated
     * based on each GeoServer service type.
     */
    @Test
    void testServiceConditionalAnnotations() {
        // This should exist in WFS service
        assertThat(context.containsBean("wfsConditionalBean")).isTrue();
        if (context.containsBean("wfsConditionalBean")) {
            ConditionalTestAutoConfiguration.ConditionalTestBean bean =
                    context.getBean("wfsConditionalBean", ConditionalTestAutoConfiguration.ConditionalTestBean.class);
            assertThat(bean.getServiceName()).isEqualTo("WFS");
        }

        // These should not exist in WFS service
        assertThat(context.containsBean("wmsConditionalBean")).isFalse();
        assertThat(context.containsBean("wcsConditionalBean")).isFalse();
        assertThat(context.containsBean("wpsConditionalBean")).isFalse();
        assertThat(context.containsBean("restConditionalBean")).isFalse();
        assertThat(context.containsBean("webUiConditionalBean")).isFalse();
    }
}
