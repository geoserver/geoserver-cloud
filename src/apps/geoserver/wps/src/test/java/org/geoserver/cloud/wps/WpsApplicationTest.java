/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.wps;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;
import org.geoserver.cloud.autoconfigure.extensions.test.ConditionalTestAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.xmlunit.assertj3.XmlAssert;

@SpringBootTest(classes = WpsApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WpsApplicationTest {

    static @TempDir Path datadir;

    private @Autowired ConfigurableApplicationContext context;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        var gwcdir = datadir.resolve("gwc");
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", gwcdir::toAbsolutePath);
    }

    private TestRestTemplate restTemplate = new TestRestTemplate("admin", "geoserver");

    @Test
    void owsGetCapabilitiesSmokeTest(@LocalServerPort int servicePort) {
        String url = "http://localhost:%d/ows?SERVICE=WPS&REQUEST=GETCAPABILITIES&VERSION=1.0.0".formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);
        Map<String, String> nscontext = Map.of("wps", "http://www.opengis.net/wps/1.0.0");
        XmlAssert.assertThat(caps).withNamespaceContext(nscontext).hasXPath("/wps:Capabilities");
    }

    @Test
    void wpsGetCapabilitiesSmokeTest(@LocalServerPort int servicePort) {
        String url = "http://localhost:%d/wps?SERVICE=WPS&REQUEST=GETCAPABILITIES&VERSION=1.0.0".formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);
        Map<String, String> nscontext = Map.of("wps", "http://www.opengis.net/wps/1.0.0");
        XmlAssert.assertThat(caps).withNamespaceContext(nscontext).hasXPath("/wps:Capabilities");
    }

    /**
     * Tests the service-specific conditional annotations.
     *
     * <p>
     * Verifies that only the WPS conditional bean is activated in this service,
     * based on the geoserver.service.wps.enabled=true property set in bootstrap.yml.
     * This test relies on the ConditionalTestAutoConfiguration class from the
     * extensions-core test-jar, which contains beans conditionally activated
     * based on each GeoServer service type.
     */
    @Test
    void testServiceConditionalAnnotations() {
        // This should exist in WPS service
        assertThat(context.containsBean("wpsConditionalBean")).isTrue();
        if (context.containsBean("wpsConditionalBean")) {
            ConditionalTestAutoConfiguration.ConditionalTestBean bean =
                    context.getBean("wpsConditionalBean", ConditionalTestAutoConfiguration.ConditionalTestBean.class);
            assertThat(bean.getServiceName()).isEqualTo("WPS");
        }

        // These should not exist in WPS service
        assertThat(context.containsBean("wfsConditionalBean")).isFalse();
        assertThat(context.containsBean("wcsConditionalBean")).isFalse();
        assertThat(context.containsBean("wmsConditionalBean")).isFalse();
        assertThat(context.containsBean("restConditionalBean")).isFalse();
        assertThat(context.containsBean("webUiConditionalBean")).isFalse();
    }
}
