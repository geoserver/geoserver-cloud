/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.filter;

import static com.github.tomakehurst.wiremock.stubbing.StubMapping.buildFrom;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.cloud.gateway.app.GatewayMvcApplication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * WireMock integration test for the {@link org.geoserver.cloud.gateway.filter.GatewaySharedAuth SharedAuth} gateway
 * filter running in a live gateway-webmvc instance.
 *
 * @since 3.0.0
 */
@SpringBootTest(
        classes = GatewayMvcApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
            "geoserver.security.gateway-shared-auth.enabled=true",
            "geoserver.extension.security.gateway-bearer-auth.enabled=true",
            "geoserver.extension.security.gateway-bearer-auth.issuer-uri=https://issuer.example.test",
            "geoserver.extension.security.gateway-bearer-auth.audience=geoserver-cloud-test",
            "geoserver.extension.security.gateway-bearer-auth.role-mappings.test-reader=PRIVATE_READER",
            "logging.level.org.geoserver.cloud.security.gateway.sharedauth=trace"
        })
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@WireMockTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(GatewaySharedAuthTest.TestJwtDecoderConfiguration.class)
class GatewaySharedAuthTest {
    // stub mappings in JSON format, see https://wiremock.org/docs/stubbing/

    /** request stub for the webui returning the logged-in username and roles as response headers */
    private static final String WEB_LOGIN_SPEC =
            """
            {
                "priority": 1,
                "request": {
                    "method": "POST",
                    "url": "/geoserver/cloud/j_spring_security_check",
                    "headers": {
                        "Accept": {"contains": "text/html"},
                        "Content-Type": {"contains": "application/x-www-form-urlencoded"}
                    }
                },
                "response": {
                    "status": 302,
                    "headers": {
                        "Content-Length": "0",
                        "Location": "http://0.0.0.0:9090/geoserver/cloud/web",
                        "Set-Cookie": ["JSESSIONID_web-ui=ABC123; Path=/; HttpOnly"],
                        "x-gsc-username": "testuser",
                        "x-gsc-roles": ["ROLE_USER","ROLE_EDITOR"]
                    }
               }
            }
            """;

    /**
     * request stub for the webui returning an empty-string on the {@literal x-gsc-username} response header, meaning to
     * log out (remove the user and roles from the session)
     */
    private static final String WEB_LOGOUT_SPEC =
            """
                {
                    "priority": 2,
                    "request": {
                        "method": "POST",
                        "url": "/j_spring_security_logout"
                    },
                    "response": {
                        "status": 302,
                        "headers": {
                            "Location": "http://0.0.0.0:9090/geoserver/cloud/web",
                            "Set-Cookie": ["session_id=abc123"],
                            "x-gsc-username": ""
                        }
                    }
                }
            """;

    /**
     * request stub for a non-webui service to check it receives the {@literal x-gsc-username} and
     * {@literal x-gsc-roles} request headers from the gateway when expected
     */
    private static final String WMS_GETCAPS =
            """
                {
                "priority": 3,
                    "request": {
                        "method": "GET",
                        "url": "/wms?request=GetCapabilities"
                    },
                    "response": {
                        "status": 200,
                        "headers": {
                            "Content-Type": "text/xml",
                            "Cache-Control": "no-cache"
                        },
                        "body": "<WMS_Capabilities/>"
                    }
                }
            """;

    /** Default response to catch up invalid mappings using the 418 status code */
    private static final String DEFAULT_RESPONSE =
            """
            {
                "priority": 10,
                "request": {"method": "ANY","urlPattern": ".*"},
                "response": {
                    "status": 418,
                    "jsonBody": { "status": "Error", "message": "I'm a teapot" },
                    "headers": {"Content-Type": "application/json"}
                }
            }
            """;

    /** saved in {@link #setUpWireMock}, to be used on {@link #registerRoutes} */
    private static WireMockRuntimeInfo wmRuntimeInfo;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestJwtDecoderConfiguration {

        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> {
                if (!"valid-test-token".equals(token)) {
                    throw new BadJwtException("Invalid test token");
                }
                return Jwt.withTokenValue(token)
                        .header("alg", "RS256")
                        .claim("iss", "https://issuer.example.test")
                        .claim("aud", List.of("geoserver-cloud-test"))
                        .claim("preferred_username", "bearer-user")
                        .claim(
                                "resource_access",
                                Map.of("gs-client", Map.of("roles", List.of("test-reader", "unmapped-role"))))
                        .build();
            };
        }
    }

    /**
     * Set up stub requests for the wiremock server. WireMock is running on a random port, so this method saves
     * {@link #wmRuntimeInfo} for {@link #registerRoutes(DynamicPropertyRegistry)}
     */
    @BeforeAll
    static void saveWireMock(WireMockRuntimeInfo runtimeInfo) {
        GatewaySharedAuthTest.wmRuntimeInfo = runtimeInfo;
    }

    /** Set up a gateway route that proxies all requests to the wiremock server with the SharedAuth filter */
    @DynamicPropertySource
    static void registerRoutes(DynamicPropertyRegistry registry) {
        String targetUrl = wmRuntimeInfo.getHttpBaseUrl();
        // Spring Cloud Gateway Server MVC uses 'spring.cloud.gateway.server.webmvc' namespace
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].id", () -> "wiremock");
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].uri", () -> targetUrl);
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].predicates[0]", () -> "Path=/**");
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].filters[0]", () -> "SharedAuth");
    }

    @Autowired
    TestRestTemplate testRestTemplate;

    private URI login;
    private URI logout;
    private URI getcapabilities;

    @BeforeEach
    void setUp(WireMockRuntimeInfo runtimeInfo) {
        // Configure TestRestTemplate to not follow redirects
        testRestTemplate
                .getRestTemplate()
                .setRequestFactory(new org.springframework.http.client.HttpComponentsClientHttpRequestFactory(
                        org.apache.hc.client5.http.impl.classic.HttpClients.custom()
                                .disableRedirectHandling()
                                .build()));

        StubMapping weblogin = buildFrom(WEB_LOGIN_SPEC);
        StubMapping weblogout = buildFrom(WEB_LOGOUT_SPEC);
        StubMapping wmscaps = buildFrom(WMS_GETCAPS);

        WireMock wireMock = runtimeInfo.getWireMock();
        wireMock.register(weblogin);
        wireMock.register(weblogout);
        wireMock.register(wmscaps);
        wireMock.register(buildFrom(DEFAULT_RESPONSE));

        login = gatewayUriOf(weblogin);
        logout = gatewayUriOf(weblogout);
        getcapabilities = gatewayUriOf(wmscaps);
    }

    /**
     * Verify that a regular stateless request (no existing session, backend returns no {@code x-gsc-*} response
     * headers) does NOT create an HTTP session. The filter must not eagerly create sessions since most services are
     * stateless.
     */
    @Test
    @Order(1)
    @DisplayName("no session created for stateless requests")
    void noSessionCreatedForStatelessRequests() {
        ResponseEntity<String> response = getCapabilities();
        assertThat(response.getBody()).startsWith("<WMS_Capabilities");

        assertThat(getJSessionIdCookie(response.getHeaders()))
                .as("No JSESSIONID cookie should be set for a stateless request")
                .isNull();
    }

    /**
     * Make a request where the caller is trying to impersonate a user with request headers {@code x-gsc-username} and
     * {@code x-gsc-roles}, verify the SharedAuth filter removes them from the proxy request and does not create a
     * session.
     */
    @Test
    @Order(2)
    @DisplayName("pre-filter avoids impersonation attempts")
    void preFilterRemovesIncomingSharedAuthHeaders(WireMockRuntimeInfo runtimeInfo) {
        ResponseEntity<String> response =
                getCapabilities("x-gsc-username", "user", "x-gsc-roles", "ROLE_1", "x-gsc-roles", "ROLE_2");
        assertThat(response.getBody()).startsWith("<WMS_Capabilities");

        LoggedRequest request =
                runtimeInfo.getWireMock().getServeEvents().getFirst().getRequest();
        assertThat(request.getUrl()).as("expected call to getcapabilities").isEqualTo(getcapabilities.toString());

        com.github.tomakehurst.wiremock.http.HttpHeaders headers = request.getHeaders();
        assertThat(headers.keys().stream().map(String::toLowerCase).collect(Collectors.toSet()))
                .as("SharedAuth filter did not remove the x-gsc-username incoming header")
                .isNotEmpty()
                .doesNotContain("x-gsc-username")
                .as("SharedAuth filter did not remove the x-gsc-roles incoming header")
                .doesNotContain("x-gsc-roles");

        assertThat(getJSessionIdCookie(response.getHeaders()))
                .as("No JSESSIONID cookie should be set when backend returns no auth headers")
                .isNull();
    }

    /**
     * Verify that a session IS created when the backend responds with {@code x-gsc-username} (i.e., on login). The
     * session is needed to store the auth info for subsequent requests.
     */
    @Test
    @Order(3)
    @DisplayName("session created when backend returns auth headers")
    void sessionCreatedOnLogin() {
        ResponseEntity<Void> response = login();

        assertThat(getJSessionIdCookie(response.getHeaders()))
                .as("JSESSIONID cookie should be set when backend returns x-gsc-username")
                .isNotNull();
    }

    /**
     * Make a request to the wms service, once the {@code x-gsc-username} and {@code x-gsc-roles} are stored in the
     * {@link jakarta.servlet.http.HttpSession}, verify the SharedAuth filter appends them as request headers to the wms
     * service proxied request
     */
    @Test
    @Order(4)
    @DisplayName("pre-filter appends user and roles headers from session")
    void preFilterAppendsRequestHeadersFromSession(WireMockRuntimeInfo runtimeInfo) {
        // preflight, make sure the webui responded with the headers and they're in the session
        ResponseEntity<Void> loginResponse = login();
        final String gatewaySessionId = getGatewaySessionId(loginResponse.getHeaders());

        // query the wms service with the gateway session id
        runtimeInfo.getWireMock().getServeEvents().clear();
        ResponseEntity<String> getcaps = getCapabilities("Cookie", "JSESSIONID=%s".formatted(gatewaySessionId));
        assertThat(getcaps.getBody()).startsWith("<WMS_Capabilities");

        // verify the wms service got the request headers
        LoggedRequest wmsRequest =
                runtimeInfo.getWireMock().getServeEvents().getFirst().getRequest();
        assertThat(wmsRequest.getUrl()).as("expected call to getcapabilities").isEqualTo(getcapabilities.toString());

        com.github.tomakehurst.wiremock.http.HttpHeaders headers = wmsRequest.getHeaders();
        HttpHeader username = headers.getHeader("x-gsc-username");
        assertThat(username)
                .as("SharedAuth filter should have added the x-gsc-username from the session")
                .isNotNull();
        assertThat(username.getValues()).isEqualTo(List.of("testuser"));

        HttpHeader roles = headers.getHeader("x-gsc-roles");
        assertThat(roles)
                .as("SharedAuth filter should have added the x-gsc-roles from the session")
                .isNotNull();
        assertThat(roles.getValues()).isEqualTo(List.of("ROLE_USER", "ROLE_EDITOR"));
    }

    /**
     * Make a request to the webui that returns the {@code x-gsc-username} and {@code x-gsc-roles} response headers,
     * verify the SharedAuth filter saves them in the {@link jakarta.servlet.http.HttpSession}
     */
    @Test
    @Order(5)
    @DisplayName("post-filter saves user and roles in session")
    void postFilterSavesUserAndRolesInSession(WireMockRuntimeInfo runtimeInfo) {
        ResponseEntity<Void> loginResponse = login();
        final String gatewaySessionId = getGatewaySessionId(loginResponse.getHeaders());

        // Make another request with the session to verify the session has the user info
        // The pre-filter will add headers from session if they exist
        runtimeInfo.getWireMock().getServeEvents().clear();
        ResponseEntity<String> getcaps = getCapabilities("Cookie", "JSESSIONID=%s".formatted(gatewaySessionId));
        assertThat(getcaps.getBody()).startsWith("<WMS_Capabilities");

        // If the post-filter correctly saved the auth info in the session, the pre-filter
        // should have added it to the proxied request
        LoggedRequest wmsRequest =
                runtimeInfo.getWireMock().getServeEvents().getFirst().getRequest();
        HttpHeader username = wmsRequest.getHeaders().getHeader("x-gsc-username");
        assertThat(username.getValues()).isEqualTo(List.of("testuser"));
    }

    @Test
    @Order(6)
    @DisplayName("post-filter clears user and roles from session on empty username response header")
    void postFilterRemovesUserAndRolesFromSessionOnEmptyUserResponseHeader(WireMockRuntimeInfo runtimeInfo) {
        // preflight, have a session and the user and roles stored
        ResponseEntity<Void> loginResponse = login();
        final String gatewaySessionId = getGatewaySessionId(loginResponse.getHeaders());

        // make a request that returns an empty string on the x-gsc-username response header
        logout(gatewaySessionId);

        // Now make a request to the wms service with the same session
        runtimeInfo.getWireMock().getServeEvents().clear();
        ResponseEntity<String> getcaps = getCapabilities("Cookie", "JSESSIONID=%s".formatted(gatewaySessionId));
        assertThat(getcaps.getBody()).startsWith("<WMS_Capabilities");

        // The pre-filter should NOT have added headers since they were cleared from the session
        LoggedRequest wmsRequest =
                runtimeInfo.getWireMock().getServeEvents().getFirst().getRequest();
        com.github.tomakehurst.wiremock.http.HttpHeaders headers = wmsRequest.getHeaders();
        assertThat(headers.keys().stream().map(String::toLowerCase).collect(Collectors.toSet()))
                .isNotEmpty()
                .as("SharedAuth filter did not clear x-gsc-username from the session")
                .doesNotContain("x-gsc-username")
                .as("SharedAuth filter did not clear x-gsc-roles from the session")
                .doesNotContain("x-gsc-roles");
    }

    /**
     * Make a call to the web-ui that returns {@code x-gsc-username} and {@code x-gsc-roles} headers, and verify the
     * SharedAuth filter does not propagate them to the response.
     */
    @Test
    @Order(7)
    @DisplayName("post-filter removes user and roles headers from the final response")
    void postFilterRemovesOutgoingSharedAuthHeaders() {
        ResponseEntity<Void> response = login();
        HttpHeaders responseHeaders = response.getHeaders();

        assertThat(responseHeaders.get("x-gsc-username"))
                .as("SharedAuth filter should have removed the x-gsc-username response header")
                .isNull();

        assertThat(responseHeaders.get("x-gsc-roles"))
                .as("SharedAuth filter should have removed the x-gsc-roles response header")
                .isNull();
    }

    @Test
    @Order(8)
    @DisplayName("valid bearer token is mapped and forwarded as trusted GeoServer identity")
    void validBearerTokenIsMappedAndForwarded(WireMockRuntimeInfo runtimeInfo) {
        ResponseEntity<String> response = getCapabilities("Authorization", "Bearer valid-test-token");
        assertThat(response.getHeaders().getFirst("x-gsc-username")).isNull();
        assertThat(response.getHeaders().getFirst("x-gsc-roles")).isNull();
        assertThat(getJSessionIdCookie(response.getHeaders())).isNull();
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");

        LoggedRequest request =
                runtimeInfo.getWireMock().getServeEvents().getFirst().getRequest();
        com.github.tomakehurst.wiremock.http.HttpHeaders headers = request.getHeaders();
        assertThat(headers.getHeader("x-gsc-username").getValues()).containsExactly("bearer-user");
        assertThat(headers.getHeader("x-gsc-roles").getValues()).containsExactly("ROLE_PRIVATE_READER");
        assertThat(headers.getHeader("Authorization").getValues()).allMatch(String::isBlank);
    }

    @Test
    @Order(9)
    @DisplayName("invalid bearer token is rejected before reaching a service")
    void invalidBearerTokenIsRejected(WireMockRuntimeInfo runtimeInfo) {
        ResponseEntity<String> response = testRestTemplate.exchange(
                getcapabilities,
                HttpMethod.GET,
                withHeaders("Authorization", "Bearer invalid-test-token"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(runtimeInfo.getWireMock().getServeEvents()).isEmpty();
    }

    /** Returns the JSESSIONID cookie value from the response, or {@code null} if not present. */
    private String getJSessionIdCookie(HttpHeaders responseHeaders) {
        List<String> cookies = responseHeaders.get("Set-Cookie");
        if (cookies == null) {
            return null;
        }
        return cookies.stream()
                .filter(c -> c.startsWith("JSESSIONID="))
                .findFirst()
                .map(cookie -> {
                    String sessionId = cookie.substring("JSESSIONID=".length());
                    return sessionId.substring(0, sessionId.indexOf(';'));
                })
                .orElse(null);
    }

    private String getGatewaySessionId(HttpHeaders responseHeaders) {
        String sessionId = getJSessionIdCookie(responseHeaders);
        assertThat(sessionId)
                .as("Expected JSESSIONID cookie in response, got: " + responseHeaders.get("Set-Cookie"))
                .isNotNull();
        return sessionId;
    }

    private URI gatewayUriOf(StubMapping mapping) {
        return URI.create(mapping.getRequest().getUrl());
    }

    ResponseEntity<Void> login() {
        HttpEntity<?> entity = withHeaders(
                "Accept", "text/html,application/xhtml+xml",
                "Content-Type", "application/x-www-form-urlencoded");
        ResponseEntity<Void> response = testRestTemplate.postForEntity(login, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        HttpHeaders headers = response.getHeaders();
        assertThat(headers.get("Location")).containsOnly("http://0.0.0.0:9090/geoserver/cloud/web");

        return response;
    }

    ResponseEntity<Void> logout(@NonNull String gatewaySessionId) {
        HttpEntity<?> entity = withHeaders(
                "Accept", "text/html,application/xhtml+xml",
                "Content-Type", "application/x-www-form-urlencoded",
                "Cookie", "JSESSIONID=%s".formatted(gatewaySessionId));
        ResponseEntity<Void> response = testRestTemplate.postForEntity(logout, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        HttpHeaders headers = response.getHeaders();
        assertThat(headers.get("Location")).containsOnly("http://0.0.0.0:9090/geoserver/cloud/web");

        return response;
    }

    ResponseEntity<String> getCapabilities(String... requestHeadersKvp) {
        HttpEntity<?> entity = withHeaders(requestHeadersKvp);
        ResponseEntity<String> response =
                testRestTemplate.exchange(getcapabilities, HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        HttpHeaders headers = response.getHeaders();
        assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_XML);

        return response;
    }

    private HttpEntity<?> withHeaders(String... headersKvp) {
        assertThat(headersKvp.length % 2).as("headers kvp shall come in pairs").isZero();
        HttpHeaders headers = new HttpHeaders();
        Iterator<String> it = Stream.of(headersKvp).iterator();
        while (it.hasNext()) {
            headers.add(it.next(), it.next());
        }
        return new HttpEntity<>(headers);
    }
}
