/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.restconfig;

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
 * <p>
 * Standard GeoServer REST expects specific servlet path and path info structures. This filter,
 * along with its associated {@link SuffixStripFilterAwareHttpServletRequest}, ensures that
 * even when running behind a gateway or in a microservice context, the REST controllers
 * receive requests in the expected format.
 * </p>
 */
class RestRequestPathInfoFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        request = RestRequestPathInfoFilter.adaptRequest((HttpServletRequest) request);
        chain.doFilter(request, response);
    }

    public static HttpServletRequest adaptRequest(HttpServletRequest request) {
        final String requestURI = request.getRequestURI();
        @SuppressWarnings("java:S1075") // base path is fixed
        final String basePath = "/rest";
        final int basePathIdx = requestURI.indexOf(basePath);
        if (basePathIdx > -1) {
            return new SuffixStripFilterAwareHttpServletRequest(request, basePath);
        }
        return request;
    }

    /**
     * An {@link HttpServletRequestWrapper} that adjusts the request URI, servlet path, and path
     * info to match what the GeoServer REST API expects.
     * <p>
     * It also overrides content-type resolution to support path extensions (e.g., .sld) when
     * the original request's Content-Type is generic or missing.
     * </p>
     */
    private static class SuffixStripFilterAwareHttpServletRequest extends HttpServletRequestWrapper {

        private HttpServletRequest request;

        final String servletPath;
        final String adjustedRequestURI;
        final String pathInfo;

        public SuffixStripFilterAwareHttpServletRequest(HttpServletRequest request, String basePath) {
            super(request);
            this.request = request;

            final String requestURI = request.getRequestURI();
            final int basePathIdx = requestURI.indexOf(basePath);

            final String contextPath = request.getContextPath();
            final String prefix = requestURI.substring(0, basePathIdx);
            if (prefix.length() > contextPath.length()) {
                // virtual service URL: strip the workspace prefix from the URI
                servletPath = prefix.substring(contextPath.length());
                adjustedRequestURI = contextPath + requestURI.substring(prefix.length());
            } else {
                servletPath = basePath;
                adjustedRequestURI = requestURI;
            }

            final String pathToBase = requestURI.substring(0, basePathIdx + basePath.length());
            pathInfo = requestURI.substring(pathToBase.length());
        }

        @Override
        public String getRequestURI() {
            return adjustedRequestURI;
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
                    int lastDot = adjustedRequestURI.lastIndexOf('.');
                    int lastSlash = adjustedRequestURI.lastIndexOf('/');
                    if (lastDot > lastSlash) {
                        ext = adjustedRequestURI.substring(lastDot + 1);
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
