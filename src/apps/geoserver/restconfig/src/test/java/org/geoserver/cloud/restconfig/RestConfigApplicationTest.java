/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.restconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_HTML;

import org.geoserver.catalog.SLDHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
abstract class RestConfigApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void before() {
        restTemplate = restTemplate.withBasicAuth("admin", "geoserver");
    }

    @Test
    void testAnnonymousForbidden() {
        restTemplate = restTemplate.withBasicAuth(null, null);
        ResponseEntity<String> response = restTemplate.getForEntity("/rest", String.class);
        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    void testGatewaySharedAuthenticationForbidden() {
        restTemplate = restTemplate.withBasicAuth(null, null);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-gsc-username", "gabe");
        headers.set("x-gsc-roles", "ROLE_USER");

        ResponseEntity<String> response;

        response = restTemplate.exchange("/rest", GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    void testGatewaySharedAuthenticationAdmin() {
        restTemplate = restTemplate.withBasicAuth(null, null);

        HttpHeaders headers = new HttpHeaders();

        headers.set("x-gsc-username", "gabe");
        headers.set("x-gsc-roles", "ADMIN");
        ResponseEntity<String> response = restTemplate.exchange("/rest", GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(OK);
    }

    @Test
    void testBasicAdminAccess() {
        testPathExtensionContentType("/rest", TEXT_HTML);
        testPathExtensionContentType("/rest/", TEXT_HTML);
        testPathExtensionContentType("/rest/index", TEXT_HTML);
    }

    @Test
    void testDefaultContentType() {
        testPathExtensionContentType("/rest/workspaces", APPLICATION_JSON);
        testPathExtensionContentType("/rest/layers", APPLICATION_JSON);
    }

    @Test
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
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(expected);
    }
}
