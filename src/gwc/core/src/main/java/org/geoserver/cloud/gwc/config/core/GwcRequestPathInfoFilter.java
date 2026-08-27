/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import java.io.IOException;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Servlet filter that proceeds with an {@link HttpServletRequestWrapper} decorator to return
 * {@link HttpServletRequestWrapper#getPathInfo() getPathInfo()} built from {@link
 * HttpServletRequestWrapper#getRequestURI() getRequestURI()}.
 *
 * <p>GWC makes heavy use of {@link HttpServletRequestWrapper#getPathInfo()}, but it returns
 * {@code null} in a spring-boot application.
 *
 * <p>Only genuine GWC dispatch paths are adapted: those where {@code /gwc} is a whole path segment
 * placed either right after the context path or after a virtual service prefix
 * ({@code /{workspace}} or {@code /{workspace}/{layer}}). Requests under another dispatcher's URL
 * space that merely contain {@code /gwc} in their data, such as the REST API's
 * {@code /rest/resource/gwc-gs.xml} or {@code /rest/resource/gwc/...}, are left untouched (issue
 * #913).
 *
 * <p>For virtual service URLs (workspace-prefixed), the behavior depends on the path type:
 *
 * <ul>
 *   <li>For {@code /gwc/service/**} paths: the workspace prefix is <b>preserved</b> in {@link
 *       HttpServletRequest#getRequestURI()} so the GeoServer Dispatcher's {@code
 *       LocalWorkspaceCallback} can detect the workspace and set {@code LocalWorkspace}/{@code
 *       LocalPublished}. The {@code GwcServiceDispatcherCallback} will then adjust {@code
 *       getContextPath()} to include the workspace, allowing {@code
 *       GeoWebCacheDispatcher.normalizeURL()} to compute the correct GWC-relative path.
 *   <li>For non-service paths ({@code /gwc/demo}, {@code /gwc/rest}, etc.): the workspace prefix
 *       is stripped from the URI so {@code GeoWebCacheDispatcher.normalizeURL()} sees {@code
 *       contextPath + /gwc/...} directly.
 * </ul>
 *
 * @since 1.0
 */
public class GwcRequestPathInfoFilter implements Filter {

    @SuppressWarnings("java:S1075") // base path is fixed
    static final String GWC_BASE_PATH = "/gwc";

    /**
     * First path segments claimed by other GeoServer dispatchers: {@code rest} is the REST API and
     * {@code web} the wicket UI. A workspace with one of these names cannot be addressed through
     * virtual service URLs anyway, the gateway routes those base paths to their own services.
     */
    private static final Set<String> RESERVED_DISPATCH_PREFIXES = Set.of("rest", "web");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        request = adaptRequest((HttpServletRequest) request);
        chain.doFilter(request, response);
    }

    public static HttpServletRequest adaptRequest(HttpServletRequest request) {
        final String requestURI = request.getRequestURI();
        final String contextPath = request.getContextPath();
        final int gwcIdx = indexOfGwcBasePath(requestURI, contextPath);
        if (gwcIdx > -1) {
            final String prefix = requestURI.substring(0, gwcIdx);
            final String afterGwc = requestURI.substring(gwcIdx + GWC_BASE_PATH.length());
            final String servletPath;
            final String adjustedRequestURI;
            if (prefix.length() > contextPath.length()) {
                servletPath = prefix.substring(contextPath.length());
                if (afterGwc.startsWith("/service")) {
                    adjustedRequestURI = requestURI;
                } else {
                    adjustedRequestURI = contextPath + requestURI.substring(prefix.length());
                }
            } else {
                servletPath = GWC_BASE_PATH;
                adjustedRequestURI = requestURI;
            }

            final String pathToGwc = requestURI.substring(0, gwcIdx + GWC_BASE_PATH.length());
            final String pathInfo = requestURI.substring(pathToGwc.length());

            return new HttpServletRequestWrapper(request) {
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
            };
        }
        return request;
    }

    /**
     * Locates {@code /gwc} where it acts as the GWC servlet base path, returning its index within
     * {@code requestURI}, or {@code -1} when the request is not a GWC one. Occurrences inside
     * longer segments ({@code /gwc-gs.xml}) or deeper in the path than a virtual service prefix
     * allows ({@code /rest/resource/gwc/...}) do not qualify.
     */
    private static int indexOfGwcBasePath(String requestURI, String contextPath) {
        final String path = requestURI.substring(contextPath.length());
        int idx = path.indexOf(GWC_BASE_PATH);
        while (idx > -1) {
            if (isWholeSegment(path, idx) && isVirtualServicePrefix(path.substring(0, idx))) {
                return contextPath.length() + idx;
            }
            idx = path.indexOf(GWC_BASE_PATH, idx + 1);
        }
        return -1;
    }

    private static boolean isWholeSegment(String path, int idx) {
        final int end = idx + GWC_BASE_PATH.length();
        return end == path.length() || path.charAt(end) == '/';
    }

    /**
     * The path before {@code /gwc} may only be empty or a virtual service prefix:
     * {@code /{workspace}} or {@code /{workspace}/{layer}}, with the workspace not being a base
     * path reserved by another dispatcher.
     */
    private static boolean isVirtualServicePrefix(String prefix) {
        if (prefix.isEmpty()) {
            return true;
        }
        String[] segments = prefix.substring(1).split("/");
        if (segments.length > 2) {
            return false;
        }
        return !RESERVED_DISPATCH_PREFIXES.contains(segments[0]);
    }
}
