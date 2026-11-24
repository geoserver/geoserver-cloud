/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.ogcapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;

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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Verifies that the OGC API Features extension and the REST API answer correctly when the WFS and REST configuration
 * services run in the same application context.
 *
 * <p>This is the combination where ownership used to depend on parse order: {@code restconfig} hands Spring MVC to
 * GeoServer's {@code RestConfiguration}, while the OGC API chain reached the same context with a
 * {@code WebMvcConfigurationSupport} subclass of its own. {@code RestConfiguration} owns the infrastructure here, and
 * the OGC API answers over the handler adapter its dispatcher builds from {@code APIConfigurationSupport}.
 */
@SpringBootTest(
        classes = GeoServerApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"geoserver.services=wfs,restconfig"})
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
class OgcApiAndRestConfigCoexistenceTest {

    static @TempDir Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", () -> datadir.resolve("gwc").toAbsolutePath());
    }

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void authenticateAsAdmin() {
        restTemplate = restTemplate.withBasicAuth("admin", "geoserver");
    }

    @Test
    void geoServerRestConfigurationOwnsSpringMvc() {
        SpringMvcOwnership.assertGeoServerRestConfigurationOwnsSpringMvc(context);
    }

    @Test
    void landingPageAnswersJson() {
        OgcApiFeaturesRequests.assertLandingPageAnswersJson(restTemplate);
    }

    @Test
    void conformanceAnswersJson() {
        OgcApiFeaturesRequests.assertConformanceAnswersJson(restTemplate);
    }

    @Test
    void restApiKeepsAnsweringJson() {
        ResponseEntity<String> response = restTemplate.getForEntity("/rest/workspaces", String.class);
        assertThat(response.getStatusCode()).as("%s", response.getBody()).isEqualTo(OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(APPLICATION_JSON))
                .isTrue();
    }
}
