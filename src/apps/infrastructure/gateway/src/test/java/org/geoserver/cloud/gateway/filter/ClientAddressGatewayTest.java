/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gateway.filter;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import java.util.Map;
import org.geoserver.cloud.gateway.GatewayApplication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * Wiremock test of the {@code X-Forwarded-For} header sent to the services by a running gateway: the client address,
 * read from the {@code X-Forwarded-For} header of the request only when a trusted proxy sent the request.
 */
@SpringBootTest(classes = GatewayApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test") // bootstrap-test.yml disables config and discovery
@WireMockTest
// The forwarded headers strategy of gateway-service.yml, and a trusted proxy address unused by the tests, which send
// their requests from 127.0.0.1
@TestPropertySource(
        properties = {"server.forward-headers-strategy=framework", "geoserver.gateway.trusted-proxies=10\\.9\\.8\\.7"})
class ClientAddressGatewayTest {

    /** saved in {@link #saveWireMock}, to be used on {@link #registerRoutes} */
    private static WireMockRuntimeInfo wmRuntimeInfo;

    @BeforeAll
    static void saveWireMock(WireMockRuntimeInfo runtimeInfo) {
        ClientAddressGatewayTest.wmRuntimeInfo = runtimeInfo;
    }

    /** Set up a gateway route that proxies all requests to the wiremock server */
    @DynamicPropertySource
    static void registerRoutes(DynamicPropertyRegistry registry) {
        String targetUrl = wmRuntimeInfo.getHttpBaseUrl();
        registry.add("spring.cloud.gateway.routes[0].id", () -> "wiremock");
        registry.add("spring.cloud.gateway.routes[0].uri", () -> targetUrl);
        registry.add("spring.cloud.gateway.routes[0].predicates[0]", () -> "Path=/**");
    }

    @Autowired
    TestRestTemplate testRestTemplate;

    @BeforeEach
    void stubEcho(WireMockRuntimeInfo runtimeInfo) {
        runtimeInfo.getWireMock().resetRequests();
        runtimeInfo.getWireMock().register(get(urlPathEqualTo("/echo")).willReturn(ok()));
    }

    @Test
    void clientAddress_connectionAddress(WireMockRuntimeInfo runtimeInfo) {
        getEcho(testRestTemplate, Map.of());

        assertThat(forwardedFor(runtimeInfo)).isEqualTo("127.0.0.1");
    }

    @Test
    void clientAddress_ignoresForwardedFor(WireMockRuntimeInfo runtimeInfo) {
        getEcho(testRestTemplate, Map.of("X-Forwarded-For", "6.6.6.6"));

        assertThat(forwardedFor(runtimeInfo)).isEqualTo("127.0.0.1");
    }

    /** The tests send their requests from 127.0.0.1. With that address trusted, the gateway honors their header. */
    @Nested
    @TestPropertySource(properties = "geoserver.gateway.trusted-proxies=127\\.0\\.0\\.1")
    class FromTrustedProxy {

        @Autowired
        TestRestTemplate trustedRestTemplate;

        @Test
        void clientAddress_fromForwardedFor(WireMockRuntimeInfo runtimeInfo) {
            getEcho(trustedRestTemplate, Map.of("X-Forwarded-For", "6.6.6.6"));

            assertThat(forwardedFor(runtimeInfo)).isEqualTo("6.6.6.6");
        }
    }

    private static void getEcho(TestRestTemplate restTemplate, Map<String, String> headers) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setAll(headers);
        HttpEntity<?> entity = new HttpEntity<>(requestHeaders);

        ResponseEntity<String> response = restTemplate.exchange("/echo", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /** The X-Forwarded-For header of the last request received by the wiremock server */
    private static String forwardedFor(WireMockRuntimeInfo runtimeInfo) {
        List<LoggedRequest> requests = runtimeInfo.getWireMock().find(getRequestedFor(urlPathEqualTo("/echo")));
        assertThat(requests).hasSize(1);
        return requests.get(0).getHeader("X-Forwarded-For");
    }
}
