/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.logging.mdc.servlet;

import com.google.common.base.Suppliers;
import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.cloud.logging.mdc.config.HttpRequestMdcConfigProperties;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that adds HTTP request-specific information to the Mapped Diagnostic Context (MDC).
 * <p>
 * This filter extracts various properties from the HTTP request and adds them to the MDC based on
 * the configuration defined in {@link HttpRequestMdcConfigProperties}. The information can include:
 * <ul>
 *   <li>Request ID</li>
 *   <li>Remote address</li>
 *   <li>Remote host</li>
 *   <li>HTTP method</li>
 *   <li>Request URL</li>
 *   <li>Query string</li>
 *   <li>Request parameters</li>
 *   <li>Session ID</li>
 *   <li>HTTP headers</li>
 *   <li>Cookies</li>
 * </ul>
 * <p>
 * The filter adds these properties to the MDC before the request is processed, making them
 * available to all logging statements executed during request processing.
 * <p>
 * This filter extends {@link OncePerRequestFilter} to ensure it's only applied once per request,
 * even in a nested dispatch scenario (e.g., forward).
 *
 * @see HttpRequestMdcConfigProperties
 * @see org.slf4j.MDC
 */
@RequiredArgsConstructor
public class HttpRequestMdcFilter extends OncePerRequestFilter {

    private final @NonNull HttpRequestMdcConfigProperties config;

    /**
     * Main filter method that processes HTTP requests and adds MDC properties.
     * <p>
     * This method adds HTTP request-specific information to the MDC before allowing the
     * request to proceed through the filter chain. The properties are added in a try-finally
     * block to ensure they're available throughout the request processing, even if an
     * exception occurs.
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
            if (request instanceof HttpServletRequest req) {
                addRequestMdcProperties(req);
            }
        } finally {
            chain.doFilter(request, response);
        }
    }

    /**
     * Adds HTTP request properties to the MDC based on configuration.
     * <p>
     * This method extracts various properties from the HTTP request and adds them to the MDC
     * based on the configuration in {@link HttpRequestMdcConfigProperties}. It handles lazy
     * evaluation of properties through suppliers to avoid unnecessary computation.
     *
     * @param req the HTTP servlet request to extract properties from
     */
    private void addRequestMdcProperties(HttpServletRequest req) {
        Supplier<HttpHeaders> headers = headers(req);
        config.id(headers)
                .remoteAddr(req::getRemoteAddr)
                .remoteHost(req::getRemoteHost)
                .method(req::getMethod)
                .url(req::getRequestURI)
                .queryString(req::getQueryString)
                .parameters(parameters(req))
                .sessionId(sessionId(req))
                .headers(headers)
                .cookies(cookies(req));
    }

    /**
     * Creates a supplier for request parameters.
     * <p>
     * This method returns a Supplier that, when invoked, will provide a MultiValueMap
     * containing the request parameters. Using a Supplier allows lazy evaluation of
     * the parameters.
     *
     * @param req the HTTP servlet request
     * @return a Supplier that provides the request parameters as a MultiValueMap
     */
    Supplier<MultiValueMap<String, String>> parameters(HttpServletRequest req) {
        return () -> {
            var map = new LinkedMultiValueMap<String, String>();
            Map<String, String[]> params = req.getParameterMap();
            params.forEach((k, v) -> map.put(k, v == null ? null : Arrays.asList(v)));
            return map;
        };
    }

    /**
     * Creates a supplier for request cookies.
     * <p>
     * This method returns a Supplier that, when invoked, will provide a MultiValueMap
     * containing the request cookies. Using a Supplier allows lazy evaluation of
     * the cookies.
     *
     * @param req the HTTP servlet request
     * @return a Supplier that provides the request cookies as a MultiValueMap
     */
    private Supplier<MultiValueMap<String, HttpCookie>> cookies(HttpServletRequest req) {
        return () -> {
            Cookie[] cookies = req.getCookies();
            var map = new LinkedMultiValueMap<String, HttpCookie>();
            if (null != cookies && cookies.length > 0) {
                for (Cookie c : cookies) {
                    map.add(c.getName(), new HttpCookie(c.getName(), c.getValue()));
                }
            }
            return map;
        };
    }

    /**
     * Creates a supplier for the session ID.
     * <p>
     * This method returns a Supplier that, when invoked, will provide the session ID
     * if a session exists, or null otherwise. Using a Supplier allows lazy evaluation
     * and avoids creating a new session if one doesn't exist.
     *
     * @param req the HTTP servlet request
     * @return a Supplier that provides the session ID or null
     */
    private Supplier<String> sessionId(HttpServletRequest req) {
        return () -> Optional.ofNullable(req.getSession(false))
                .map(HttpSession::getId)
                .orElse(null);
    }

    /**
     * Creates a memoized supplier for request headers.
     * <p>
     * This method returns a Supplier that, when invoked, will provide the HTTP headers.
     * The result is memoized (cached) to avoid repeated computation if the headers are
     * accessed multiple times.
     *
     * @param req the HTTP servlet request
     * @return a memoized Supplier that provides the request headers
     */
    private Supplier<HttpHeaders> headers(HttpServletRequest req) {
        return Suppliers.memoize(buildHeaders(req));
    }

    /**
     * Builds a supplier that constructs HttpHeaders from the request.
     * <p>
     * This method creates a Guava Supplier that, when invoked, will extract all headers
     * from the request and place them in a Spring HttpHeaders object.
     *
     * @param req the HTTP servlet request
     * @return a Supplier that builds HttpHeaders from the request
     */
    private com.google.common.base.Supplier<HttpHeaders> buildHeaders(HttpServletRequest req) {
        return () -> {
            HttpHeaders headers = new HttpHeaders();
            Streams.stream(req.getHeaderNames().asIterator())
                    .forEach(name -> headers.put(name, headerValue(name, req)));
            return headers;
        };
    }

    /**
     * Extracts values for a specific header from the request.
     * <p>
     * This method retrieves all values for a given header name from the request
     * and returns them as a List of Strings.
     *
     * @param name the header name
     * @param req the HTTP servlet request
     * @return a List of header values, or an empty list if none exist
     */
    private List<String> headerValue(String name, HttpServletRequest req) {
        Enumeration<String> values = req.getHeaders(name);
        return (null == values)
                ? List.of()
                : Streams.stream(values.asIterator()).toList();
    }
}
