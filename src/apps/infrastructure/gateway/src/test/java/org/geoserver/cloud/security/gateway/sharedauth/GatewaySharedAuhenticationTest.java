/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security.gateway.sharedauth;

import static com.github.tomakehurst.wiremock.stubbing.StubMapping.buildFrom;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import lombok.NonNull;

import org.geoserver.cloud.gateway.GatewayApplication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.EnableWebFluxConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;
import org.springframework.web.server.session.WebSessionStore;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wiremock integration test for a running gateway with {@link GatewaySharedAuhenticationPreFilter}
 * and {@link GatewaySharedAuhenticationPostFilter}
 */
@SpringBootTest(
        classes = GatewayApplication.class, //
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"geoserver.security.gateway-shared-auth.enabled=true"})
@ActiveProfiles("test") // bootstrap-test.yml disables config and discovery
@WireMockTest
// @TestMethodOrder is not really needed, just used to run tests in the workflow order, but tests
// are isolated
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatewaySharedAuhenticationTest {
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
                        "Content-Type": {"equalTo": "application/x-www-form-urlencoded"}
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
     * request stub for the webui returning an empty-string on the {@literal x-gsc-username}
     * response header, meaning to log out (remove the user and roles from the session)
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

    /**
     * Set up stub requests for the wiremock server. WireMock is running on a random port, so this
     * method saves {@link #wmRuntimeInfo} for {@link #registerRoutes(DynamicPropertyRegistry)}
     */
    @BeforeAll
    static void saveWireMock(WireMockRuntimeInfo runtimeInfo) {
        GatewaySharedAuhenticationTest.wmRuntimeInfo = runtimeInfo;
    }

    /** Set up a gateway route that proxies all requests to the wiremock server */
    @DynamicPropertySource
    static void registerRoutes(DynamicPropertyRegistry registry) {
        String targetUrl = wmRuntimeInfo.getHttpBaseUrl();
        registry.add("spring.cloud.gateway.routes[0].id", () -> "wiremock");
        registry.add("spring.cloud.gateway.routes[0].uri", () -> targetUrl);
        registry.add("spring.cloud.gateway.routes[0].predicates[0]", () -> "Path=/**");
    }

    @Autowired TestRestTemplate testRestTemplate;

    /**
     * Concrete implementation of {@link WebSessionManager} as created by {@link
     * EnableWebFluxConfiguration#webSessionManager()} so we can access {@link
     * DefaultWebSessionManager#getSessionStore()}
     */
    @Autowired DefaultWebSessionManager webSessionManager;

    private URI login;
    private URI logout;
    private URI getcapabilities;

    @BeforeEach
    void setUp(WireMockRuntimeInfo runtimeInfo) throws Exception {
        StubMapping weblogin = buildFrom(WEB_LOGIN_SPEC);
        StubMapping weblogout = buildFrom(WEB_LOGOUT_SPEC);
        StubMapping wmscaps = buildFrom(WMS_GETCAPS);

        WireMock wireMock = runtimeInfo.getWireMock();
        wireMock.register(weblogin);
        wireMock.register(weblogout);
        wireMock.register(wmscaps);
        wireMock.register(buildFrom(DEFAULT_RESPONSE));

        login = gatewayUriOf(runtimeInfo, weblogin);
        logout = gatewayUriOf(runtimeInfo, weblogout);
        getcapabilities = gatewayUriOf(runtimeInfo, wmscaps);
    }

    /**
     * Make a request where the caller is trying to impersonate a user with request headers {@code
     * x-gsc-username} and {@code x-gsc-roles}, verify {@link GatewaySharedAuhenticationPreFilter}
     * removes them from the proxy request
     */
    @Test
    @Order(1)
    @DisplayName("pre-filter avoids impersonation attempts")
    void preFilterRemovesIncomingSharedAuthHeaders(WireMockRuntimeInfo runtimeInfo) {
        ResponseEntity<String> response =
                getCapabilities(
                        "x-gsc-username", "user", "x-gsc-roles", "ROLE_1", "x-gsc-roles", "ROLE_2");
        assertThat(response.getBody()).startsWith("<WMS_Capabilities");

        LoggedRequest request = runtimeInfo.getWireMock().getServeEvents().getFirst().getRequest();
        assertThat(request.getUrl())
                .as("expected call to getcapabilities")
                .isEqualTo(getcapabilities.toString());

        com.github.tomakehurst.wiremock.http.HttpHeaders headers = request.getHeaders();
        assertThat(headers.keys().stream().map(String::toLowerCase).collect(Collectors.toSet()))
                .as(
                        "GatewaySharedAuhenticationPreFilter did not remove the x-gsc-username incoming header")
                .isNotEmpty()
                .doesNotContain("x-gsc-username")
                .as(
                        "GatewaySharedAuhenticationPreFilter did not remove the x-gsc-roles incoming header")
                .doesNotContain("x-gsc-roles");
    }

    /**
     * Make a request to the wms service, once the {@code x-gsc-username} and {@code x-gsc-roles}
     * are stored in the the {@link WebSession}, verify {@link GatewaySharedAuhenticationPreFilter}
     * appends them as request headers to the wms service proxied request
     */
    @Test
    @Order(2)
    @DisplayName("pre-filter appends user and roles headers from session")
    void preFilterAppendsRequestHeadersFromSession(WireMockRuntimeInfo runtimeInfo) {
        // preflight, make sure the webui responsed with the headers and they're in the
        // session
        ResponseEntity<Void> login = login();
        final String gatewaySessionId = getGatewaySessionId(login.getHeaders());
        assertUserAndRolesStoredInSession(gatewaySessionId);

        // query the wms service with the gateway session id
        runtimeInfo.getWireMock().getServeEvents().clear();
        ResponseEntity<String> getcaps =
                getCapabilities("Cookie", "SESSION=%s".formatted(gatewaySessionId));
        assertThat(getcaps.getBody()).startsWith("<WMS_Capabilities");

        // verify the wms service got the request headers
        LoggedRequest wmsRequest =
                runtimeInfo.getWireMock().getServeEvents().getFirst().getRequest();
        assertThat(wmsRequest.getUrl())
                .as("expected call to getcapabilities")
                .isEqualTo(getcapabilities.toString());

        com.github.tomakehurst.wiremock.http.HttpHeaders headers = wmsRequest.getHeaders();
        HttpHeader username = headers.getHeader("x-gsc-username");
        assertThat(username)
                .as(
                        "GatewaySharedAuhenticationPreFilter should have added the x-gsc-username from the session")
                .isNotNull();
        assertThat(username.getValues()).isEqualTo(List.of("testuser"));

        HttpHeader roles = headers.getHeader("x-gsc-roles");
        assertThat(roles)
                .as(
                        "GatewaySharedAuhenticationPreFilter should have added the x-gsc-ROLES from the session")
                .isNotNull();
        assertThat(roles.getValues()).isEqualTo(List.of("ROLE_USER", "ROLE_EDITOR"));
    }

    /**
     * Make a request to the webui that returns the {@code x-gsc-username} and {@code x-gsc-roles}
     * response headers, verify {@link GatewaySharedAuhenticationPostFilter} saves them in the
     * {@link WebSession}
     */
    @Test
    @Order(3)
    @DisplayName("post-filter saves user and roles in session")
    void postFilterSavesUserAndRolesInSession(WireMockRuntimeInfo runtimeInfo) {
        ResponseEntity<Void> login = login();
        final String gatewaySessionId = getGatewaySessionId(login.getHeaders());

        assertUserAndRolesStoredInSession(gatewaySessionId);
    }

    @Test
    @Order(4)
    @DisplayName("post-filter clears user and roles from session on empty username response header")
    void postFilterRemovesUserAndRolesFromSessionOnEmptyUserResponseHeader(
            WireMockRuntimeInfo runtimeInfo) {
        // preflight, have a session and the user and roles stored
        ResponseEntity<Void> login = login();
        final String gatewaySessionId = getGatewaySessionId(login.getHeaders());
        assertUserAndRolesStoredInSession(gatewaySessionId);

        // make a request that returns and empty string on the x-gsc-username response header
        logout(gatewaySessionId);
        Map<String, Object> attributes = getSessionAttributes(gatewaySessionId);
        assertThat(attributes)
                .as(
                        "GatewaySharedAuhenticationPostFilter did not remove x-gsc-username from the session")
                .doesNotContainKey("x-gsc-username")
                .as(
                        "GatewaySharedAuhenticationPostFilter did not remove x-gsc-roles from the session")
                .doesNotContainKey("x-gsc-roles");
    }

    /**
     * Make a call to the web-ui that returns {@code x-gsc-username} and {@code x-gsc-roles}
     * headers, and verify {@link GatewaySharedAuhenticationPostFilter} does not propagate them to
     * the response.
     */
    @Test
    @Order(5)
    @DisplayName("post-filter removes user and roles headers from the final response")
    void postFilterRemovesOutgoingSharedAuthHeaders(WireMockRuntimeInfo runtimeInfo) {
        ResponseEntity<Void> response = login();
        HttpHeaders responseHeaders = response.getHeaders();
        assertThat(responseHeaders)
                .as(
                        "GatewaySharedAuhenticationGlobalFilter should have removed the x-gsc-username response header")
                .doesNotContainKey("x-gsc-username")
                .as(
                        "GatewaySharedAuhenticationGlobalFilter should have removed the x-gsc-roles response header")
                .doesNotContainKey("x-gsc-roles");
    }

    private String getGatewaySessionId(HttpHeaders responseHeaders) {
        List<String> cookies = responseHeaders.get("Set-Cookie");
        String cookie =
                cookies.stream().filter(c -> c.startsWith("SESSION=")).findFirst().orElseThrow();
        String sessionId = cookie.substring("SESSION=".length());
        sessionId = sessionId.substring(0, sessionId.indexOf(';'));
        return sessionId;
    }

    private void assertUserAndRolesStoredInSession(final String gatewaySessionId) {
        Map<String, Object> attributes = getSessionAttributes(gatewaySessionId);
        assertThat(attributes)
                .containsEntry("x-gsc-username", "testuser")
                .containsEntry("x-gsc-roles", List.of("ROLE_USER", "ROLE_EDITOR"));
    }

    private Map<String, Object> getSessionAttributes(final String gatewaySessionId) {
        WebSessionStore sessionStore = webSessionManager.getSessionStore();
        WebSession session = sessionStore.retrieveSession(gatewaySessionId).block();
        Map<String, Object> attributes = session.getAttributes();
        return attributes;
    }

    private URI gatewayUriOf(WireMockRuntimeInfo runtimeInfo, StubMapping mapping) {
        return URI.create(mapping.getRequest().getUrl());
    }

    ResponseEntity<Void> login() {
        HttpEntity<?> entity =
                withHeaders( //
                        "Accept", "text/html,application/xhtml+xml", //
                        "Content-Type", "application/x-www-form-urlencoded");
        ResponseEntity<Void> response = testRestTemplate.postForEntity(login, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        HttpHeaders headers = response.getHeaders();

        assertThat(headers)
                .containsEntry("Location", List.of("http://0.0.0.0:9090/geoserver/cloud/web"));

        return response;
    }

    ResponseEntity<Void> logout(@NonNull String gatewaySessionId) {
        HttpEntity<?> entity =
                withHeaders( //
                        "Accept", "text/html,application/xhtml+xml", //
                        "Content-Type", "application/x-www-form-urlencoded",
                        "Cookie", "SESSION=%s".formatted(gatewaySessionId));
        ResponseEntity<Void> response = testRestTemplate.postForEntity(logout, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        HttpHeaders headers = response.getHeaders();

        assertThat(headers)
                .containsEntry("Location", List.of("http://0.0.0.0:9090/geoserver/cloud/web"));

        return response;
    }

    ResponseEntity<String> getCapabilities(String... headersKvp) {
        HttpEntity<?> entity = withHeaders(headersKvp);
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
