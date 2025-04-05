/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.accesslog;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A Servlet filter for logging HTTP request access.
 * <p>
 * This filter is similar to Spring's {@link CommonsRequestLoggingFilter} but uses SLF4J for logging
 * and provides more configuration options through {@link AccessLogFilterConfig}. It captures the
 * following information about each request:
 * <ul>
 *   <li>HTTP method (GET, POST, etc.)</li>
 *   <li>URI path</li>
 *   <li>Status code</li>
 * </ul>
 * <p>
 * The filter leverages MDC (Mapped Diagnostic Context) for enriched logging. By configuring this
 * filter along with the MDC filters (like {@link org.geoserver.cloud.logging.mdc.servlet.HttpRequestMdcFilter}),
 * you can include detailed request information in your access logs.
 * <p>
 * This filter is positioned with {@link Ordered#HIGHEST_PRECEDENCE} + 3 to ensure it executes
 * after the MDC context is set up but before most application processing occurs.
 *
 * @see AccessLogFilterConfig
 * @see CommonsRequestLoggingFilter
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class AccessLogServletFilter extends OncePerRequestFilter {

    private final @NonNull AccessLogFilterConfig config;

    public AccessLogServletFilter(@NonNull AccessLogFilterConfig conf) {
        this.config = conf;
    }

    /**
     * Main filter method that processes HTTP requests and logs access information.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Allows the request to proceed through the filter chain</li>
     *   <li>After the response is complete, captures the method, URI, and status code</li>
     *   <li>Logs the request using the configured patterns and log levels</li>
     * </ol>
     * <p>
     * The method is designed to always log the request, even if an exception occurs during processing,
     * by using a try-finally block.
     *
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @param filterChain the filter chain to execute
     * @throws ServletException if a servlet exception occurs
     * @throws IOException if an I/O exception occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } finally {
            String uri = request.getRequestURI();
            String method = request.getMethod();
            int statusCode = response.getStatus();
            config.log(method, statusCode, uri);
        }
    }
}
