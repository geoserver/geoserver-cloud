/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.restconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_HTML;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.nio.file.Path;
import org.geoserver.cloud.geoserver.GeoServerApplication;
import org.geoserver.cloud.geoserver.SpringMvcOwnership;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Verifies that the GeoServer REST API and GWC's own REST API answer correctly when both services run in the same
 * application context.
 *
 * <p>Enabling {@code restconfig} hands Spring MVC over to GeoServer's {@code RestConfiguration}, which replaces the
 * handler mapping and message converters Spring Boot would otherwise configure for every controller in the context,
 * GWC's included.
 */
@SpringBootTest(
        classes = GeoServerApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"geoserver.services=restconfig,gwc"})
@ActiveProfiles("datadir")
@AutoConfigureTestRestTemplate
class RestAndGwcCoexistenceTest {

    static @TempDir Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", () -> datadir.resolve("gwc").toAbsolutePath());
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ConfigurableApplicationContext context;

    @BeforeEach
    void authenticateAsAdmin() {
        restTemplate = restTemplate.withBasicAuth("admin", "geoserver");
    }

    @Test
    void geoServerRestConfigurationOwnsSpringMvc() {
        SpringMvcOwnership.assertGeoServerRestConfigurationOwnsSpringMvc(context);
    }

    @Test
    void geoServerRestApiUsesItsOwnConverters() {
        assertContentType("/rest", TEXT_HTML);
        assertContentType("/rest/workspaces", APPLICATION_JSON);
        assertContentType("/rest/workspaces.xml", APPLICATION_XML);
    }

    @Test
    void gwcRestApiKeepsAnsweringJson() {
        ResponseEntity<String> response = assertContentType("/gwc/rest/layers", APPLICATION_JSON);
        JsonElement parsed = JsonParser.parseString(response.getBody());
        assertThat(parsed.isJsonArray()).isTrue();
    }

    @Test
    void gwcRestApiKeepsHonoringPathExtensions() {
        assertContentType("/gwc/rest/layers.json", APPLICATION_JSON);
        assertContentType("/gwc/rest/layers.xml", APPLICATION_XML);
    }

    private ResponseEntity<String> assertContentType(String uri, MediaType expected) {
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        assertThat(response.getStatusCode())
                .as("%s -> %s", uri, response.getBody())
                .isEqualTo(OK);
        MediaType contentType = response.getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.isCompatibleWith(expected))
                .as("%s: expected %s got %s", uri, expected, contentType)
                .isTrue();
        return response;
    }
}
