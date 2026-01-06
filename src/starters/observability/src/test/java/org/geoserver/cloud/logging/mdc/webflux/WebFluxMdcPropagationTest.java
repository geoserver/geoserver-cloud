/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.logging.mdc.webflux;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.geoserver.cloud.logging.mdc.config.AuthenticationMdcConfigProperties;
import org.geoserver.cloud.logging.mdc.config.HttpRequestMdcConfigProperties;
import org.geoserver.cloud.logging.mdc.config.SpringEnvironmentMdcConfigProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Simple tests for MDC propagation in WebFlux.
 * <p>
 * Tests basic functionality of the WebFlux MDC filter.
 */
class WebFluxMdcPropagationTest {

    private MDCWebFilter mdcWebFilter;
    private HttpRequestMdcConfigProperties httpConfig;
    private AuthenticationMdcConfigProperties authConfig;
    private SpringEnvironmentMdcConfigProperties envConfig;
    private Environment env;
    private Optional<BuildProperties> buildProps;

    @BeforeEach
    void setup() {
        // Clear MDC before each test
        MDC.clear();

        // Initialize config objects
        httpConfig = new HttpRequestMdcConfigProperties();
        authConfig = new AuthenticationMdcConfigProperties();
        envConfig = new SpringEnvironmentMdcConfigProperties();

        // Configure properties to include
        httpConfig.setMethod(true);
        httpConfig.setUrl(true);
        httpConfig.setRemoteAddr(true);
        httpConfig.setId(true);

        // Mock environment
        env = mock(Environment.class);
        when(env.getProperty("spring.application.name")).thenReturn("test-application");

        // Empty build properties
        buildProps = Optional.empty();

        // Create filter
        mdcWebFilter = new MDCWebFilter(authConfig, httpConfig, envConfig, env, buildProps);
    }

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void testBasicFilter() {
        // Create mock exchange
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);

        // Configure request
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/test-path"));
        when(request.getHeaders()).thenReturn(HttpHeaders.EMPTY);
        when(request.getRemoteAddress()).thenReturn(new java.net.InetSocketAddress("127.0.0.1", 8080));
        when(exchange.getPrincipal()).thenReturn(Mono.empty());

        // Simple test chain that just returns empty
        WebFilterChain chain = _ -> Mono.empty();

        // Execute filter
        Mono<Void> result = mdcWebFilter.filter(exchange, chain);

        // Verify execution completes without errors
        StepVerifier.create(result).expectComplete().verify(Duration.ofSeconds(1));
    }
}
