/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.restconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.TEXT_HTML;

import java.nio.file.Path;
import org.geoserver.cloud.geoserver.GeoServerApplication;
import org.geoserver.cloud.geoserver.SpringMvcOwnership;
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
 * Verifies that the Wicket web UI keeps answering correctly when it runs alongside the REST configuration service in
 * the same application context.
 *
 * <p>Enabling {@code restconfig} hands Spring MVC over to GeoServer's {@code RestConfiguration}, which replaces the
 * handler mapping and Spring Boot's default static-resource handlers (for {@code classpath:/static},
 * {@code classpath:/META-INF/resources} and webjars) for the whole context. The web UI does not depend on those: Wicket
 * serves its pages and resources through its own servlet filter, mounted at {@code /web/*} independently of which
 * {@code WebMvcConfigurationSupport} owns Spring MVC.
 */
@SpringBootTest(
        classes = GeoServerApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"geoserver.services=webui,restconfig"})
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
class WebUiAndRestConfigCoexistenceTest {

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

    @Test
    void geoServerRestConfigurationOwnsSpringMvc() {
        SpringMvcOwnership.assertGeoServerRestConfigurationOwnsSpringMvc(context);
    }

    @Test
    void wicketPageRenders() {
        ResponseEntity<String> response = assertContentType("/web/", TEXT_HTML);
        assertThat(response.getBody()).contains("GeoServer");
    }

    /**
     * The {@code geoserver-cloud.css} theme is contributed through a Wicket {@code HeaderContribution} scoped to
     * {@code GeoServerBasePage}, and served at Wicket's package-resource URL, independently of Spring Boot's own
     * static-resource handlers.
     */
    @Test
    void wicketPackageResourceIsServed() {
        MediaType textCss = MediaType.valueOf("text/css");
        ResponseEntity<String> response = assertContentType(
                "/web/wicket/resource/org.geoserver.web.GeoServerBasePage/geoserver-cloud.css", textCss);
        assertThat(response.getBody()).contains(".unused");
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
