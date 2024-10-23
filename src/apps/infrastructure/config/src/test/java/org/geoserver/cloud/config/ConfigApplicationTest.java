/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"native", "test"})
class ConfigApplicationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUri;

    @BeforeEach
    void setup() {
        baseUri = "http://localhost:%d/test-service".formatted(port);
    }

    @Test
    void testNoProfile() throws Exception {
        assertThat(
                this.restTemplate.getForEntity(baseUri, String.class).getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void testDefaultProfile() throws Exception {
        String uri = baseUri + "/default";
        ResponseEntity<String> response = this.restTemplate.getForEntity(uri, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        String config = response.getBody();
        log.info(config);

        String expected =
                """
			{"name":"test-service","profiles":["default"],\
			"label":null,"version":null,"state":null,\
			"propertySources":[{"name":"classpath:/config/test-service.yml","source":{"spring.application.name":"geoserver"}}]}
			""";
        JSONAssert.assertEquals(expected, config, JSONCompareMode.LENIENT);
    }

    @Test
    void testProfile() throws Exception {
        String uri = baseUri + "/profile1";
        ResponseEntity<String> response = this.restTemplate.getForEntity(uri, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        String config = response.getBody();
        log.info(config);
        String expected =
                """
			{"name":"test-service","profiles":["profile1"],\
			"label":null,"version":null,"state":null,\
			"propertySources":[\
			{"name":"classpath:/config/test-service-profile1.yml","source":{"spring.application.name":"geoserver-profile1"}},\
			{"name":"classpath:/config/test-service.yml","source":{"spring.application.name":"geoserver"}}\
			]}
			""";
        JSONAssert.assertEquals(expected, config, JSONCompareMode.LENIENT);
    }
}
