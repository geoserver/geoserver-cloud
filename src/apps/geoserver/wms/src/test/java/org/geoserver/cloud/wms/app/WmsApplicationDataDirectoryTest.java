/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.wms.app;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.xmlunit.assertj3.XmlAssert;

@SpringBootTest(
        classes = WmsApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"geoserver.backend.data-directory.enabled=true", "gwc.wms-integration=true"})
@ActiveProfiles({"test"})
class WmsApplicationDataDirectoryTest extends WmsApplicationTest {

    static @TempDir Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        var gwcdir = datadir.resolve("gwc");
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", gwcdir::toAbsolutePath);
    }

    private String baseURL;

    private TestRestTemplate restTemplate = new TestRestTemplate("admin", "geoserver");

    @BeforeEach
    void setUp(@LocalServerPort int servicePort) {
        baseURL = "http://localhost:%d/ows".formatted(servicePort);
    }

    @Test
    void getCapabilitiesSmokeTest() {
        String url = baseURL + "?SERVICE=WMS&REQUEST=GETCAPABILITIES&VERSION=1.3.0";
        String caps = restTemplate.getForObject(url, String.class);
        Map<String, String> nscontext = Map.of("wms", "http://www.opengis.net/wms");
        XmlAssert.assertThat(caps).withNamespaceContext(nscontext).hasXPath("/wms:WMS_Capabilities");
    }
}
