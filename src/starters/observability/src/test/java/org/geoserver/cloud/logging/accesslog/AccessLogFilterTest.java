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

/** Tests for the Servlet-based {@link AccessLogServletFilter}. */
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
}
