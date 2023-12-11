/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wfs.app;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@SpringBootTest(classes = WfsApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WfsApplicationTest {

    static @TempDir Path tmpdir;
    static Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) throws IOException {
        datadir = Files.createDirectory(tmpdir.resolve("datadir"));
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
    }

    private TestRestTemplate restTemplate = new TestRestTemplate("admin", "geoserver");

    @Test
    void owsGetCapabilitiesSmokeTest(@LocalServerPort int servicePort) {
        String url =
                "http://localhost:%d/ows?SERVICE=WFS&REQUEST=GETCAPABILITIES&VERSION=1.1.0"
                        .formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);
        Map<String, String> nscontext = Map.of("wfs", "http://www.opengis.net/wfs");
        XmlAssert.assertThat(caps)
                .withNamespaceContext(nscontext)
                .hasXPath("/wfs:WFS_Capabilities");
    }

    @Test
    void wfsGetCapabilitiesSmokeTest(@LocalServerPort int servicePort) {
        String url =
                "http://localhost:%d/wfs?SERVICE=WFS&REQUEST=GETCAPABILITIES&VERSION=1.1.0"
                        .formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);
        Map<String, String> nscontext = Map.of("wfs", "http://www.opengis.net/wfs");
        XmlAssert.assertThat(caps)
                .withNamespaceContext(nscontext)
                .hasXPath("/wfs:WFS_Capabilities");
    }
}
