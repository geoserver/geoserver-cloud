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

import org.json.simple.parser.ParseException;
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
public class GeoWebCacheApplicationTest {

    @Autowired private TestRestTemplate restTemplate;

    @BeforeEach
    void before() {
        restTemplate = restTemplate.withBasicAuth("admin", "geoserver");
        String rootUri = restTemplate.getRootUri();
        assertThat(rootUri).isNotEmpty();
    }

    @Test
    public void testRESTDefaultContentType() throws ParseException {
        ResponseEntity<String> response =
                testGetRequestContentType("/gwc/rest/layers", APPLICATION_JSON);
        JsonElement parsed = JsonParser.parseString(response.getBody());
        assertThat(parsed.isJsonArray());
    }

    @Test
    public void testRESTPathExtensionContentNegotiation() {
        ResponseEntity<String> response =
                testGetRequestContentType("/gwc/rest/layers.json", APPLICATION_JSON);
        JsonElement parsed = JsonParser.parseString(response.getBody());
        assertThat(parsed.isJsonArray());

        testGetRequestContentType("/gwc/rest/layers.xml", APPLICATION_XML);
    }

    protected ResponseEntity<String> testGetRequestContentType(String uri, MediaType expected) {
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(expected);
        return response;
    }
}
