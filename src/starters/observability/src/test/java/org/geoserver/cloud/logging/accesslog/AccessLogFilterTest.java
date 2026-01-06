/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.logging.accesslog;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests for the access log filters.
 * <p>
 * This test class covers both the Servlet-based {@link AccessLogServletFilter} and
 * the WebFlux-based {@link AccessLogWebfluxFilter}.
 */
class AccessLogFilterTest {

    private AccessLogFilterConfig config;
    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;
    private FilterChain servletChain;

    @BeforeEach
    void setup() {
        // Clear MDC before each test
        MDC.clear();

        // Initialize config object
        config = new AccessLogFilterConfig();

        // Create mocks for servlet components
        servletRequest = mock(HttpServletRequest.class);
        servletResponse = mock(HttpServletResponse.class);
        servletChain = mock(FilterChain.class);

        // Configure basic request properties
        when(servletRequest.getMethod()).thenReturn("GET");
        when(servletRequest.getRequestURI()).thenReturn("/api/data");
        when(servletResponse.getStatus()).thenReturn(200);
    }

    @Test
    void testServletFilterWithMatchingUri() throws ServletException, IOException {
        // Configure access log to log all paths at info level
        config.getInfo().add(Pattern.compile(".*"));

        // Create filter and execute
        AccessLogServletFilter filter = new AccessLogServletFilter(config);
        filter.doFilterInternal(servletRequest, servletResponse, servletChain);

        // Verify filter chain was called
        verify(servletChain).doFilter(servletRequest, servletResponse);

        // It's difficult to verify log output directly, but we can verify that the
        // filter executed without errors and called the chain
    }

    @Test
    void testServletFilterWithNonMatchingUri() throws ServletException, IOException {
        // Configure access log with pattern that won't match
        config.getInfo().add(Pattern.compile("/admin/.*"));

        // Create filter and execute
        AccessLogServletFilter filter = new AccessLogServletFilter(config);
        filter.doFilterInternal(servletRequest, servletResponse, servletChain);

        // Verify filter chain was called
        verify(servletChain).doFilter(servletRequest, servletResponse);
    }

    @Test
    void testServletFilterWithDifferentLogLevels() throws ServletException, IOException {
        // Configure access log with different patterns for different log levels
        config.getTrace().add(Pattern.compile("/trace/.*"));
        config.getDebug().add(Pattern.compile("/debug/.*"));
        config.getInfo().add(Pattern.compile("/info/.*"));

        // Test with a request that matches info level
        when(servletRequest.getRequestURI()).thenReturn("/info/test");

        // Create filter and execute
        AccessLogServletFilter filter = new AccessLogServletFilter(config);
        filter.doFilterInternal(servletRequest, servletResponse, servletChain);

        // Verify filter chain was called
        verify(servletChain).doFilter(servletRequest, servletResponse);
    }

    @Test
    void testServletFilterWithErrorStatus() throws ServletException, IOException {
        // Configure access log to log all paths at info level
        config.getInfo().add(Pattern.compile(".*"));

        // Configure response with error status
        when(servletResponse.getStatus()).thenReturn(500);

        // Create filter and execute
        AccessLogServletFilter filter = new AccessLogServletFilter(config);
        filter.doFilterInternal(servletRequest, servletResponse, servletChain);

        // Verify filter chain was called
        verify(servletChain).doFilter(servletRequest, servletResponse);
    }

    @Test
    void testWebfluxFilterWithMatchingUri() {
        // Configure access log to log all paths at info level
        config.getInfo().add(Pattern.compile(".*"));

        // Mock request and response
        ServerWebExchange exchange1 = mock(ServerWebExchange.class);
        ServerHttpRequest request1 = mock(ServerHttpRequest.class);
        ServerHttpResponse response1 = mock(ServerHttpResponse.class);

        // Configure exchange
        when(exchange1.getRequest()).thenReturn(request1);
        when(exchange1.getResponse()).thenReturn(response1);
        when(request1.getURI()).thenReturn(java.net.URI.create("http://localhost/api/data"));
        when(request1.getMethod()).thenReturn(HttpMethod.GET);
        when(response1.getRawStatusCode()).thenReturn(200);

        // Configure chain
        WebFilterChain chain1 = _ -> Mono.empty();

        // Create filter and execute
        AccessLogWebfluxFilter filter = new AccessLogWebfluxFilter(config);
        Mono<Void> result = filter.filter(exchange1, chain1);

        // Verify filter executes without errors
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    void testWebfluxFilterWithErrorStatus() {
        // Configure access log to log all paths at info level
        config.getInfo().add(Pattern.compile(".*"));

        // Mock request and response
        ServerWebExchange exchange2 = mock(ServerWebExchange.class);
        ServerHttpRequest request2 = mock(ServerHttpRequest.class);
        ServerHttpResponse response2 = mock(ServerHttpResponse.class);

        // Configure exchange with error status
        when(exchange2.getRequest()).thenReturn(request2);
        when(exchange2.getResponse()).thenReturn(response2);
        when(request2.getURI()).thenReturn(java.net.URI.create("http://localhost/api/data"));
        when(request2.getMethod()).thenReturn(HttpMethod.GET);
        when(response2.getRawStatusCode()).thenReturn(500);

        // Configure chain
        WebFilterChain chain2 = _ -> Mono.empty();

        // Create filter and execute
        AccessLogWebfluxFilter filter = new AccessLogWebfluxFilter(config);
        Mono<Void> result = filter.filter(exchange2, chain2);

        // Verify filter executes without errors
        StepVerifier.create(result).verifyComplete();
    }
}
