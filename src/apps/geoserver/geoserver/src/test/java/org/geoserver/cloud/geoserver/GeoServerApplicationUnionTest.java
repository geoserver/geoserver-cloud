/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.geoserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.util.Locale;
import org.geoserver.rest.SuffixStripFilter;
import org.geoserver.wcs.DefaultWebCoverageService100;
import org.geoserver.wfs.DefaultWebFeatureService;
import org.geoserver.wms.WMS;
import org.geoserver.wps.DefaultWebProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Boots {@link GeoServerApplication} with every service enabled, the default {@code geoserver.services=all}, and
 * verifies the union of services coexists in a single application context without the bean conflicts each service was
 * absorbed one at a time to avoid.
 */
@SpringBootTest(classes = GeoServerApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GeoServerApplicationUnionTest {

    static @TempDir Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", () -> datadir.resolve("gwc").toAbsolutePath());
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void allServicesActive() {
        assertThat(context.getBeansOfType(WMS.class)).isNotEmpty();
        assertThat(context.getBeansOfType(DefaultWebFeatureService.class)).isNotEmpty();
        assertThat(context.getBeansOfType(DefaultWebCoverageService100.class)).isNotEmpty();
        assertThat(context.getBeansOfType(DefaultWebProcessingService.class)).isNotEmpty();
        assertThat(context.containsBean("gwcDispatcherController")).isTrue();
        assertThat(context.containsBean("restRequestPathInfoFilter")).isTrue();
        assertThat(context.getBeansOfType(org.geoserver.web.GeoServerApplication.class))
                .isNotEmpty();
    }

    /**
     * {@code suffixStripFilter} is shared by the REST configuration and GWC services (see
     * {@code SuffixStripFilterAutoConfiguration}); running both together must not reintroduce the bean name conflict
     * the shared definition replaced.
     */
    @Test
    void singleSuffixStripFilterInUnion() {
        assertThat(context.getBeanNamesForType(SuffixStripFilter.class)).hasSize(1);
    }

    /**
     * The web UI and GWC services each contribute a root ({@code /}) redirect, mutually exclusive by design: with both
     * services enabled, only the web UI's redirect must remain.
     */
    @Test
    void singleRootMappingInUnion() {
        assertThat(context.containsBean("webUiRootRedirectMapping")).isTrue();
        assertThat(context.containsBean("gwcRootRedirectController")).isFalse();
    }

    @Test
    void rootRedirectsToWebUi(@LocalServerPort int port) throws Exception {
        assertRedirectsToWebUi(port, "/");
    }

    // GeoServer redirects the root to this page, part of its war file and absent from this application
    @Test
    void indexHtmlRedirectsToWebUi(@LocalServerPort int port) throws Exception {
        assertRedirectsToWebUi(port, "/index.html");
    }

    /**
     * With {@code restconfig} enabled in the union, GeoServer's {@code RestConfiguration} must own Spring MVC, exactly
     * as when the REST configuration service runs alone.
     */
    @Test
    void geoServerRestConfigurationOwnsSpringMvc() {
        SpringMvcOwnership.assertGeoServerRestConfigurationOwnsSpringMvc(context);
    }

    /**
     * Gateway shared authentication trusts {@code x-gsc-*} request headers, which is only safe behind a gateway that
     * strips them from client requests. Without a gateway, the combined app leaves it off by default (see
     * {@link MonolithSafeDefaultsTest}), so forged headers must not authenticate the request, and the response must not
     * echo them back either.
     */
    @Test
    void forgedSharedAuthHeadersIgnored(@LocalServerPort int port) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-gsc-username", "admin");
        headers.add("x-gsc-roles", "ROLE_ADMINISTRATOR");
        ResponseEntity<String> response = new TestRestTemplate()
                .exchange(
                        "http://localhost:%d/rest/workspaces".formatted(port),
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class);
        assertThat(response.getStatusCode().value()).isIn(401, 403);
        assertThat(response.getHeaders().headerNames())
                .noneMatch(h -> h.toLowerCase(Locale.ROOT).startsWith("x-gsc"));
    }

    private void assertRedirectsToWebUi(int port, String path) throws IOException, InterruptedException {
        URI requested = URI.create("http://localhost:%d%s".formatted(port, path));
        HttpRequest request = HttpRequest.newBuilder(requested).build();
        HttpResponse<Void> response = HttpClient.newHttpClient().send(request, BodyHandlers.discarding());
        assertThat(response.statusCode()).as(path).isEqualTo(302);
        // relative: the browser resolves it against the address it requested, which may be the one of a proxy
        assertThat(response.headers().firstValue("Location")).as(path).hasValue("web/");
    }
}
