/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.core.rest;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.Styles;
import org.geoserver.rest.SuffixStripFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Filter that adapts the incoming request to meet the expectations of the GeoServer REST API.
 *
 * <p>Standard GeoServer REST expects specific servlet path and path info structures. This filter, along with its
 * associated {@link SuffixStripFilterAwareHttpServletRequest}, ensures that even when running behind a gateway or in a
 * microservice context, the REST controllers receive requests in the expected format.
 *
 * <p>A request is adapted when {@code /rest} is the first path segment after the context path. Matching the whole first
 * segment keeps REST API requests whose path contains another base path as data, such as
 * {@code /rest/resource/gwc-gs.xml}, from being mistaken for GeoWebCache requests (issue #913), while GWC-dispatched
 * URLs such as {@code /gwc/rest/**} and {@code /{workspace}/gwc/rest/**} are left alone because their first segment is
 * not {@code rest}.
 */
public class RestRequestPathInfoFilter implements Filter {

    @SuppressWarnings("java:S1075") // base path is fixed
    static final String REST_BASE_PATH = "/rest";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        request = RestRequestPathInfoFilter.adaptRequest((HttpServletRequest) request);
        chain.doFilter(request, response);
    }

    public static HttpServletRequest adaptRequest(HttpServletRequest request) {
        if (isRestApiRequest(request)) {
            return new SuffixStripFilterAwareHttpServletRequest(request, REST_BASE_PATH);
        }
        return request;
    }

    private static boolean isRestApiRequest(HttpServletRequest request) {
        String pathAfterContext =
                request.getRequestURI().substring(request.getContextPath().length());
        if (!pathAfterContext.startsWith(REST_BASE_PATH)) {
            return false;
        }
        int basePathEnd = REST_BASE_PATH.length();
        return pathAfterContext.length() == basePathEnd || pathAfterContext.charAt(basePathEnd) == '/';
    }

    /**
     * An {@link HttpServletRequestWrapper} that adjusts the servlet path and path info to match what the GeoServer REST
     * API expects: the servlet path is the {@code /rest} base path and the path info is whatever follows it.
     *
     * <p>It also overrides content-type resolution to support path extensions (e.g., .sld) when the original request's
     * Content-Type is generic or missing.
     */
    private static class SuffixStripFilterAwareHttpServletRequest extends HttpServletRequestWrapper {

        private HttpServletRequest request;

        final String servletPath;
        final String pathInfo;

        public SuffixStripFilterAwareHttpServletRequest(HttpServletRequest request, String basePath) {
            super(request);
            this.request = request;

            final String requestURI = request.getRequestURI();
            final String contextPath = request.getContextPath();

            servletPath = basePath;
            pathInfo = requestURI.substring(contextPath.length() + basePath.length());
        }

        @Override
        public String getServletPath() {
            return servletPath;
        }

        @Override
        public String getPathInfo() {
            return pathInfo;
        }

        @Override
        public String getHeader(String name) {
            if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
                return getContentType();
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
                return Collections.enumeration(Collections.singletonList(getContentType()));
            }
            return super.getHeaders(name);
        }

        @Override
        public String getContentType() {
            String contentType = super.getContentType();
            if (contentType == null
                    || contentType.startsWith(MediaType.TEXT_PLAIN_VALUE)
                    || contentType.startsWith(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
                String ext = (String) request.getAttribute(SuffixStripFilter.EXTENSION_ATTRIBUTE);
                if (ext == null) {
                    String requestURI = request.getRequestURI();
                    int lastDot = requestURI.lastIndexOf('.');
                    int lastSlash = requestURI.lastIndexOf('/');
                    if (lastDot > lastSlash) {
                        ext = requestURI.substring(lastDot + 1);
                    }
                }
                if (ext != null) {
                    if ("sld".equalsIgnoreCase(ext)) {
                        return SLDHandler.MIMETYPE_10;
                    }
                    final String extension = ext;
                    Optional<StyleHandler> handler = Styles.handlers().stream()
                            .filter(h -> h.getFileExtension().equals(extension))
                            .findFirst();
                    if (handler.isPresent()) {
                        StyleHandler h = handler.orElseThrow();
                        return h.getVersions().get(0).toString();
                    }
                }
            }
            return contentType;
        }
    }
}
