/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.geoserver.catalog.GeoServerCatalogTestData;
import org.geoserver.gwc.controller.GwcUrlHandlerMapping;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GeoWebCacheApplicationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private ApplicationContext context;

    @TempDir private static Path tmpPath;

    @BeforeAll
    static void initializeDatadir() throws URISyntaxException, IOException {
        assertThat(tmpPath).isNotNull();
        GeoServerCatalogTestData.unzipGeoserverCatalogTestData(tmpPath);
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry)
            throws URISyntaxException, IOException {
        File tmpFilePath = tmpPath.toFile();
        registry.add("geoserver.backend.data-directory.location", tmpFilePath::toString);
    }

    @BeforeEach
    void before() {
        restTemplate = restTemplate.withBasicAuth("admin", "geoserver");
        String rootUri = restTemplate.getRootUri();
        assertThat(rootUri).isNotEmpty();
    }

    @Test
    void testRESTDefaultContentType() {
        ResponseEntity<String> response =
                testGetRequestContentType("/gwc/rest/layers", APPLICATION_JSON);
        JsonElement parsed = JsonParser.parseString(response.getBody());
        assertThat(parsed.isJsonArray()).isTrue();
    }

    @Test
    void testRESTPathExtensionContentNegotiation() {
        ResponseEntity<String> response =
                testGetRequestContentType("/gwc/rest/layers.json", APPLICATION_JSON);
        JsonElement parsed = JsonParser.parseString(response.getBody());
        assertThat(parsed.isJsonArray()).isTrue();

        testGetRequestContentType("/gwc/rest/layers.xml", APPLICATION_XML);
    }

    @Test
    void testGwcUrlHandlerMappingArePresentInTheClasspath() {
        assertThat(context.isTypeMatch("gwcDemoUrlHandlerMapping", GwcUrlHandlerMapping.class))
                .as("expected a bean gwcDemoUrlHandlerMapping of type GwcUrlHandlerMapping")
                .isTrue();
        assertThat(context.isTypeMatch("gwcRestWebUrlHandlerMapping", GwcUrlHandlerMapping.class))
                .as("expected a bean gwcRestWebUrlHandlerMapping of type GwcUrlHandlerMapping")
                .isTrue();
    }

    @Test
    void testGeneralGwcHome() {
        ResponseEntity<String> response = testGetRequestContentType("/gwc/home", TEXT_HTML);
        assertThat(response.getHeaders().getContentType()).isNotEqualTo(APPLICATION_XML);
        assertThat(response.getBody()).doesNotContain("ows:Exception");
    }

    @Test
    void testGeneralGwcDemo() {
        ResponseEntity<String> response = testGetRequestContentType("/gwc/demo", TEXT_HTML);
        assertThat(response.getHeaders().getContentType()).isNotEqualTo(APPLICATION_XML);
        assertThat(response.getBody()).doesNotContain("ows:Exception");
    }

    @Test
    void testGeneralGwcDemoWithNameSpaceLayer() {
        ResponseEntity<String> response =
                testGetRequestContentType("/gwc/demo/ne:boundary_lines", TEXT_HTML);
        assertThat(response.getHeaders().getContentType()).isNotEqualTo(APPLICATION_XML);
        assertThat(response.getBody()).doesNotContain("ows:Exception");
    }

    @Test
    void testGeneralGwcServiceWmts() {
        ResponseEntity<String> response =
                testGetRequestContentType("/gwc/service/wmts?REQUEST=GetCapabilities", TEXT_XML);
        assertThat(response.getBody()).doesNotContain("ows:Exception");
    }

    @Test
    void testGwcDemoPrefixedUrls() {
        ResponseEntity<String> response = testGetRequestContentType("/ne/gwc/demo", TEXT_HTML);
        assertThat(response.getHeaders().getContentType()).isNotEqualTo(APPLICATION_XML);
        assertThat(response.getBody()).doesNotContain("ows:Exception");
    }

    @Test
    void testGwcDemoWorkspacePrefixedUrlsLayer() {
        ResponseEntity<String> response =
                testGetRequestContentType("/ne/gwc/demo/boundary_lines", TEXT_HTML);
        assertThat(response.getHeaders().getContentType()).isNotEqualTo(APPLICATION_XML);
        assertThat(response.getBody()).doesNotContain("ows:Exception");
    }

    @Test
    void testGwcDemoWorkspacePrefixedUrlsWorkspaceLayer() {
        ResponseEntity<String> response =
                testGetRequestContentType("/ne/gwc/demo/ne:boundary_lines", TEXT_HTML);
        assertThat(response.getHeaders().getContentType()).isNotEqualTo(APPLICATION_XML);
        assertThat(response.getBody()).doesNotContain("ows:Exception");
    }

    @Test
    void testGwcServicePrefixedUrl() {
        ResponseEntity<String> response =
                testGetRequestContentType("/ne/gwc/service/wmts?REQUEST=GetCapabilities", TEXT_XML);
        assertThat(response.getBody()).doesNotContain("ows:Exception");
    }

    protected ResponseEntity<String> testGetRequestContentType(String uri, MediaType expected) {
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().isCompatibleWith(expected))
                .as(
                        String.format(
                                "expected content-type %s to be compatible with %s",
                                response.getHeaders().getContentType(), expected))
                .isTrue();
        return response;
    }
}
