/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import java.io.IOException;
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

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        request = adaptRequest((HttpServletRequest) request);
        chain.doFilter(request, response);
    }

    @SuppressWarnings("java:S1075")
    public static HttpServletRequest adaptRequest(HttpServletRequest request) {
        final String requestURI = request.getRequestURI();
        final String gwcBasePath = "/gwc";
        final int gwcIdx = requestURI.indexOf(gwcBasePath);
        if (gwcIdx > -1) {
            final String contextPath = request.getContextPath();
            final String prefix = requestURI.substring(0, gwcIdx);
            final String afterGwc = requestURI.substring(gwcIdx + gwcBasePath.length());
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
                servletPath = gwcBasePath;
                adjustedRequestURI = requestURI;
            }

            final String pathToGwc = requestURI.substring(0, gwcIdx + gwcBasePath.length());
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
}
