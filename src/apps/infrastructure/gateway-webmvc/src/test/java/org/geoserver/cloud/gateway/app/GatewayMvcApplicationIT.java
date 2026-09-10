/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for gateway-webmvc using a live echo-server container to verify StripBasePath, SecureHeaders, and
 * CORS end-to-end.
 *
 * @since 3.0.0
 */
@SpringBootTest(classes = GatewayMvcApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@Testcontainers(disabledWithoutDocker = true)
class GatewayMvcApplicationIT {

    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> echoServer = new GenericContainer<>("jmalloc/echo-server").withExposedPorts(8080);

    @DynamicPropertySource
    static void registerRoutes(DynamicPropertyRegistry registry) {
        String echoUri = "http://%s:%d".formatted(echoServer.getHost(), echoServer.getMappedPort(8080));

        // Route: StripBasePath + SecureHeaders
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].id", () -> "echo-strip");
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].uri", () -> echoUri);
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].predicates[0]", () -> "Path=/geoserver/cloud/**");
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].filters[0]", () -> "StripBasePath=/geoserver/cloud");
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].filters[1]", () -> "SecureHeaders");

        // Route: direct access with SecureHeaders
        String r1 = "spring.cloud.gateway.server.webmvc.routes[1]";
        registry.add(r1 + ".id", () -> "echo-direct");
        registry.add(r1 + ".uri", () -> echoUri);
        registry.add(r1 + ".predicates[0]", () -> "Path=/echo/**");
        registry.add(r1 + ".filters[0]", () -> "SecureHeaders");

        // Route: points to a closed port on localhost to test ProxyExceptionFilter.
        // Connection is refused immediately, no timeout needed.
        String r2 = "spring.cloud.gateway.server.webmvc.routes[2]";
        registry.add(r2 + ".id", () -> "dead-backend");
        registry.add(r2 + ".uri", () -> "http://localhost:1"); // port 1: always refused
        registry.add(r2 + ".predicates[0]", () -> "Path=/dead/**");

        // CORS configuration
        registry.add(
                "spring.cloud.gateway.server.webmvc.globalcors.cors-configurations.[/**].allowed-origins[0]",
                () -> "https://allowed.example.com");
        registry.add(
                "spring.cloud.gateway.server.webmvc.globalcors.cors-configurations.[/**].allowed-methods[0]",
                () -> "GET");
        registry.add(
                "spring.cloud.gateway.server.webmvc.globalcors.cors-configurations.[/**].allowed-methods[1]",
                () -> "POST");
        registry.add(
                "spring.cloud.gateway.server.webmvc.globalcors.cors-configurations.[/**].allowed-methods[2]",
                () -> "OPTIONS");
    }

    @Autowired
    TestRestTemplate testRestTemplate;

    // --- StripBasePath tests ---

    @Test
    void stripBasePath_stripsPrefix() {
        ResponseEntity<String> response =
                testRestTemplate.getForEntity("/geoserver/cloud/ows?service=wfs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).contains("GET /ows?service=wfs").doesNotContain("/geoserver/cloud/ows");
    }

    @Test
    void stripBasePath_deepPath() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/geoserver/cloud/wms/reflect", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).contains("GET /wms/reflect").doesNotContain("/geoserver/cloud/wms");
    }

    // Regression tests for #693: a bare '=' inside a query value (CQL_FILTER=a=1, viewparams=a:b=c) made the
    // upstream stripPrefix rebuild of the URI throw, and the gateway answered 500 to a request the service accepts.

    @Test
    void stripBasePath_keepsBareEqualsSignInQuery() {
        ResponseEntity<String> response = getVerbatim("/geoserver/cloud/ows?CQL_FILTER=1=1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(decodedRequestLine(response.getBody())).isEqualTo("GET /ows?CQL_FILTER=1=1 HTTP/1.1");
        assertThat(response.getBody()).contains("X-Forwarded-Prefix: /geoserver/cloud");
    }

    @Test
    void stripBasePath_keepsPercentEncodedQuery() {
        ResponseEntity<String> response = getVerbatim("/geoserver/cloud/ows?layers=a%3Db%20c");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(decodedRequestLine(response.getBody())).isEqualTo("GET /ows?layers=a=b c HTTP/1.1");
    }

    /** Sends the path and query exactly as given, bypassing the URI template encoding of the String overloads. */
    private ResponseEntity<String> getVerbatim(String pathAndQuery) {
        URI uri = URI.create(testRestTemplate.getRootUri() + pathAndQuery);
        return testRestTemplate.getForEntity(uri, String.class);
    }

    /** The request line as echoed by the backend, percent-decoded. */
    private static String decodedRequestLine(String echoedRequest) {
        String requestLine = echoedRequest
                .lines()
                .filter(line -> line.startsWith("GET ") || line.startsWith("POST "))
                .findFirst()
                .orElseThrow();
        return URLDecoder.decode(requestLine, StandardCharsets.UTF_8);
    }

    // --- SecureHeaders tests ---

    @Test
    void secureHeaders_present() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/echo/test", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        HttpHeaders headers = response.getHeaders();
        assertThat(headers.getFirst("X-Frame-Options")).isNotNull();
        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(headers.getFirst("Strict-Transport-Security")).isNotNull();
        assertThat(headers.getFirst("Referrer-Policy")).isNotNull();
    }

    // --- CORS tests ---

    @Test
    void cors_preflight_allowed() {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Origin", "https://allowed.example.com");
        requestHeaders.set("Access-Control-Request-Method", "GET");
        HttpEntity<?> entity = new HttpEntity<>(requestHeaders);

        ResponseEntity<String> response =
                testRestTemplate.exchange("/echo/test", HttpMethod.OPTIONS, entity, String.class);

        assertThat(response.getHeaders().getFirst("Access-Control-Allow-Origin"))
                .isEqualTo("https://allowed.example.com");
    }

    @Test
    void cors_simpleRequest_allowed() {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Origin", "https://allowed.example.com");
        HttpEntity<?> entity = new HttpEntity<>(requestHeaders);

        ResponseEntity<String> response = testRestTemplate.exchange("/echo/test", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Access-Control-Allow-Origin"))
                .isEqualTo("https://allowed.example.com");
    }

    @Test
    void cors_disallowedOrigin() {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Origin", "https://evil.example.com");
        HttpEntity<?> entity = new HttpEntity<>(requestHeaders);

        ResponseEntity<String> response = testRestTemplate.exchange("/echo/test", HttpMethod.GET, entity, String.class);

        assertThat(response.getHeaders().getFirst("Access-Control-Allow-Origin"))
                .isNull();
    }

    // --- Content-Type preservation tests ---
    // Regression tests: the gateway must not alter Content-Type headers when proxying requests.
    // Spring Boot's CharacterEncodingFilter (HttpEncodingAutoConfiguration) appends ';charset=UTF-8'
    // to all Content-Type headers, which breaks downstream services that rely on exact media type
    // matching (e.g. GeoServer's CoverageStoreFileController rejects 'application/zip;charset=UTF-8').

    @Test
    void contentType_applicationZip_preservedOnPut() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        HttpEntity<byte[]> entity = new HttpEntity<>(new byte[] {0x50, 0x4b, 0x03, 0x04}, headers);

        ResponseEntity<String> response = testRestTemplate.exchange("/echo/test", HttpMethod.PUT, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String echoedRequest = response.getBody();
        assertThat(echoedRequest).contains("Content-Type: application/zip").doesNotContain("charset");
    }

    @Test
    void contentType_applicationZip_preservedOnPost() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        HttpEntity<byte[]> entity = new HttpEntity<>(new byte[] {0x50, 0x4b, 0x03, 0x04}, headers);

        ResponseEntity<String> response =
                testRestTemplate.exchange("/echo/test", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String echoedRequest = response.getBody();
        assertThat(echoedRequest).contains("Content-Type: application/zip").doesNotContain("charset");
    }

    @Test
    void contentType_textPlain_preservedOnPost() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> entity = new HttpEntity<>("/opt/data/file.tif", headers);

        ResponseEntity<String> response =
                testRestTemplate.exchange("/echo/test", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String echoedRequest = response.getBody();
        assertThat(echoedRequest).contains("Content-Type: text/plain").doesNotContain("charset");
    }

    @Test
    void contentType_applicationJson_preservedOnPut() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"key\":\"value\"}", headers);

        ResponseEntity<String> response = testRestTemplate.exchange("/echo/test", HttpMethod.PUT, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String echoedRequest = response.getBody();
        assertThat(echoedRequest).contains("Content-Type: application/json").doesNotContain("charset");
    }

    // --- ProxyExceptionFilter tests ---
    // Verify the gateway returns 502 instead of hanging or dumping a stack trace
    // when a backend service is unreachable.

    @Test
    void proxyError_returns502_whenBackendUnreachable() {
        ResponseEntity<String> response = testRestTemplate.getForEntity("/dead/test", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void proxyError_returns502_onPostToUnreachableBackend() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"key\":\"value\"}", headers);

        ResponseEntity<String> response =
                testRestTemplate.exchange("/dead/test", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }
}
