/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import jakarta.servlet.http.HttpServletRequest;
import org.geoserver.catalog.Catalog;
import org.geoserver.gwc.controller.GwcUrlHandlerMapping;
import org.springframework.http.server.RequestPath;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.ServletRequestPathUtils;

/**
 * Extends {@link GwcUrlHandlerMapping} to fix virtual service URL handling with Spring 6's {@link
 * org.springframework.web.util.pattern.PathPattern PathPattern}-based request matching.
 *
 * <p>Spring 6 uses the cached parsed request path from {@link ServletRequestPathUtils} for pattern
 * matching, ignoring the {@code lookupPath} string parameter passed to {@code
 * lookupHandlerMethod}. The upstream {@link GwcUrlHandlerMapping} strips the workspace prefix from
 * the lookupPath string but doesn't update the cached parsed path, causing virtual service URLs to
 * return 404.
 *
 * <p>This subclass updates the cached parsed path to the workspace-stripped path before delegating
 * to the parent's {@code lookupHandlerMethod}, and restores it afterwards.
 */
public class CloudGwcUrlHandlerMapping extends GwcUrlHandlerMapping {

    public CloudGwcUrlHandlerMapping(Catalog catalog, String gwcUrlPattern) {
        super(catalog, gwcUrlPattern);
    }

    @Override
    @SuppressWarnings("java:S1874")
    protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
        int gwcRestBaseIndex = lookupPath.indexOf(GWC_URL_PATTERN);
        if (gwcRestBaseIndex == -1) {
            return null;
        }

        // Strip the workspace prefix to get the path matching controller patterns
        String strippedPath = lookupPath.substring(gwcRestBaseIndex);
        String contextPath = request.getContextPath();

        // Save the original cached parsed path set by DispatcherServlet
        Object originalParsedPath = request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);

        // Update the cached parsed request path for Spring 6 PathPattern matching.
        // The parent will pass the stripped lookupPath string to super.lookupHandlerMethod,
        // but PathPattern matching reads from this request attribute instead.
        RequestPath parsedStrippedPath = RequestPath.parse(contextPath + strippedPath, contextPath);
        request.setAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE, parsedStrippedPath);

        try {
            return super.lookupHandlerMethod(lookupPath, request);
        } finally {
            if (originalParsedPath != null) {
                request.setAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE, originalParsedPath);
            } else {
                request.removeAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
            }
        }
    }
}
