/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GeoWebCacheApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void before() {
        restTemplate = restTemplate.withBasicAuth("admin", "geoserver");
        String rootUri = restTemplate.getRootUri();
        assertThat(rootUri).isNotEmpty();
    }

    @Test
    void testRESTDefaultContentType() {
        ResponseEntity<String> response = testGetRequestContentType("/gwc/rest/layers", APPLICATION_JSON);
        JsonElement parsed = JsonParser.parseString(response.getBody());
        assertThat(parsed.isJsonArray()).isTrue();
    }

    @Test
    void testRESTPathExtensionContentNegotiation() {
        ResponseEntity<String> response = testGetRequestContentType("/gwc/rest/layers.json", APPLICATION_JSON);
        JsonElement parsed = JsonParser.parseString(response.getBody());
        assertThat(parsed.isJsonArray()).isTrue();

        testGetRequestContentType("/gwc/rest/layers.xml", APPLICATION_XML);
    }

    protected ResponseEntity<String> testGetRequestContentType(String uri, MediaType expected) {
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(expected);
        return response;
    }

    @Test
    void testPostSeedDoesNotThrowAmbiguousHandlerMapping() {
        String payload =
                """
                <seedRequest>
                  <name>workspace:layer</name>
                  <srs><number>3857</number></srs>
                  <zoomStart>0</zoomStart>
                  <zoomStop>8</zoomStop>
                  <format>image/png</format>
                  <type>reseed</type>
                  <threadCount>2</threadCount>
                </seedRequest>
                """;
        String uri = "/gwc/rest/seed/workspace:layer.xml";

        ResponseEntity<String> response = restTemplate.postForEntity(URI.create(uri), payload, String.class);
        HttpStatus statusCode = response.getStatusCode();
        String body = response.getBody();

        // SeedService will throw a 500 error when the layer is not found
        // it's ok we just need to check it doesn't result in an "ambiguous handler mapping" error
        // see org.geoserver.cloud.gwc.config.services.SeedController
        assertThat(statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body).contains("Unknown layer workspace:layer");
    }
}
