/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for serving GeoServer at the root path with {@code geoserver.base-path=/}.
 *
 * <p>The base path is spliced textually into route predicates and the root redirect target (e.g.
 * {@code Path=${basepath}/wms}, {@code RedirectTo=302, ${basepath}/web/}). A raw value of {@code /} would produce
 * {@code //wms} patterns that never match and a scheme-relative {@code Location: //web/} that browsers resolve to
 * {@code http://web/}. The gateway normalizes the value at property-resolution time: {@code /} and trailing slashes
 * reduce to the canonical empty form, meaning "serve at the root path".
 *
 * <p>Routes replicate the shapes used in the shipped {@code gateway.yml}.
 *
 * @since 3.1.0
 */
@SpringBootTest(classes = GatewayMvcApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@Testcontainers(disabledWithoutDocker = true)
class GatewayMvcRootBasePathIT {

    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> echoServer = new GenericContainer<>("jmalloc/echo-server").withExposedPorts(8080);

    @DynamicPropertySource
    static void registerRoutes(DynamicPropertyRegistry registry) {
        String echoUri = "http://%s:%d".formatted(echoServer.getHost(), echoServer.getMappedPort(8080));

        registry.add("geoserver.base-path", () -> "/");
        registry.add("basepath", () -> "${geoserver.base-path}");

        String r0 = "spring.cloud.gateway.server.webmvc.routes[0]";
        registry.add(r0 + ".id", () -> "root-redirect-to-webui");
        registry.add(r0 + ".uri", () -> "no://op");
        registry.add(r0 + ".predicates[0]", () -> "Path=/,${basepath},${basepath}/");
        registry.add(r0 + ".filters[0]", () -> "RedirectTo=302, ${basepath}/web/");

        String r1 = "spring.cloud.gateway.server.webmvc.routes[1]";
        registry.add(r1 + ".id", () -> "echo");
        registry.add(r1 + ".uri", () -> echoUri);
        registry.add(r1 + ".predicates[0]", () -> "Path=${basepath}/echo/**");
        registry.add(r1 + ".filters[0]", () -> "StripBasePath=${basepath}");
        registry.add(r1 + ".filters[1]", () -> "SecureHeaders");
    }

    @Autowired
    TestRestTemplate testRestTemplate;

    @Test
    void rootBasePath_routesMatchAtRoot() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/echo/test?service=wfs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("GET /echo/test?service=wfs");
    }

    @Test
    void rootBasePath_redirectsRootToLocalWebPath() {
        // the raw base path "/" would expand the redirect target to the scheme-relative "//web/",
        // which browsers resolve to http://web/; the normalized base path yields a host-relative Location
        ResponseEntity<String> response = testRestTemplate.getForEntity("/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("/web/");
    }
}
