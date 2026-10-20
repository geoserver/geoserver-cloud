/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gateway.filter;

import static org.geoserver.cloud.logging.mdc.config.HttpRequestMdcConfigProperties.REQUEST_ID_HEADERS;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lets only trusted proxies set the request id of the logs.
 *
 * <p>The gateway takes the request id from the request headers, and creates a new one if there is none. This filter
 * hides the request id headers unless the request comes from a trusted proxy. The gateway then creates its own id.
 *
 * <p>The check uses the address of the connection, as seen by the servlet container, and not the remote address of the
 * request, see {@link TrustedProxies#isTrustedConnection}.
 *
 * <p>It must run before the {@link org.geoserver.cloud.logging.mdc.servlet.HttpRequestMdcFilter}, ordered at
 * {@code HIGHEST_PRECEDENCE + 2}: that filter reads the request id headers to fill the MDC.
 *
 * @see RequestIdHeadersFilter
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestIdTrustFilter extends OncePerRequestFilter {

    private final TrustedProxies trustedProxies;

    public RequestIdTrustFilter(TrustedProxies trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (trustedProxies.isTrustedConnection(request)) {
            chain.doFilter(request, response);
        } else {
            chain.doFilter(new RequestIdHeadersHidingRequest(request), response);
        }
    }

    /** Hides the request id headers from the next filters, and from the requests proxied to the services */
    private static class RequestIdHeadersHidingRequest extends HttpServletRequestWrapper {

        RequestIdHeadersHidingRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            if (isRequestIdHeader(name)) {
                return null;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (isRequestIdHeader(name)) {
                return Collections.emptyEnumeration();
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            names.removeIf(RequestIdHeadersHidingRequest::isRequestIdHeader);
            return Collections.enumeration(names);
        }

        private static boolean isRequestIdHeader(String name) {
            return REQUEST_ID_HEADERS.stream().anyMatch(name::equalsIgnoreCase);
        }
    }
}
