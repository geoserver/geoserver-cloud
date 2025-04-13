/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.logging.mdc.servlet;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that cleans up the MDC (Mapped Diagnostic Context) after request processing.
 * <p>
 * This filter ensures that the MDC is cleared after each request is processed, preventing
 * MDC properties from leaking between requests. This is especially important in servlet
 * containers that reuse threads for handling multiple requests.
 * <p>
 * The filter has the {@link Ordered#HIGHEST_PRECEDENCE} to ensure it wraps all other filters
 * in the chain. This positioning guarantees that any MDC cleanup happens regardless of where
 * in the filter chain an exception might occur.
 * <p>
 * This filter extends {@link OncePerRequestFilter} to ensure it's only applied once per request,
 * even in a nested dispatch scenario (e.g., forward).
 *
 * @see org.slf4j.MDC
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MDCCleaningFilter extends OncePerRequestFilter {

    /**
     * Main filter method that ensures MDC cleanup after request processing.
     * <p>
     * This method allows the request to proceed through the filter chain and then
     * clears the MDC in a finally block to ensure cleanup happens even if an exception
     * occurs during request processing.
     *
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @param chain the filter chain to execute
     * @throws ServletException if a servlet exception occurs
     * @throws IOException if an I/O exception occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
