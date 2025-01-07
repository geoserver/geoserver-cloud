/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.restconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_HTML;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.geoserver.catalog.SLDHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class RestConfigApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    static @TempDir Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) throws IOException {
        var gwcdir = datadir.resolve("gwc");
        if (!Files.exists(gwcdir)) {
            Files.createDirectory(gwcdir);
        }
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", gwcdir::toAbsolutePath);
    }

    @BeforeEach
    void before() throws Exception {
        restTemplate = restTemplate.withBasicAuth("admin", "geoserver");
    }

    /**
     * REVISIT: for some reason, running the REST API tests right after starting off
     * an empty data directory produce a 403 forbidden response. We're hence forcing
     * the order of the tests and the reload of the context for the time being
     */
    @Test
    @Order(1)
    @DirtiesContext
    void smokeTest() {
        assertTrue(true);
    }

    @Test
    @Order(2)
    @DirtiesContext
    void testDefaultContentType() {
        testPathExtensionContentType("/rest/workspaces", APPLICATION_JSON);
        testPathExtensionContentType("/rest/layers", APPLICATION_JSON);
    }

    @Test
    @Order(3)
    @DirtiesContext
    void testPathExtensionContentNegotiation() {
        testPathExtensionContentType("/rest/styles/line.json", APPLICATION_JSON);
        testPathExtensionContentType("/rest/styles/line.xml", APPLICATION_XML);
        testPathExtensionContentType("/rest/styles/line.html", TEXT_HTML);
        testPathExtensionContentType("/rest/styles/line.sld", MediaType.valueOf(SLDHandler.MIMETYPE_10));

        testPathExtensionContentType("/rest/workspaces.html", TEXT_HTML);
        testPathExtensionContentType("/rest/workspaces.xml", APPLICATION_XML);
        testPathExtensionContentType("/rest/workspaces.json", APPLICATION_JSON);
    }

    protected void testPathExtensionContentType(String uri, MediaType expected) {
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(expected);
    }
}
