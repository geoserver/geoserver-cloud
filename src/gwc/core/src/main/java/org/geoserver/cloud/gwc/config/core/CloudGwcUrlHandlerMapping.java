/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.Catalog;
import org.geoserver.gwc.controller.GwcUrlHandlerMapping;
import org.springframework.http.server.RequestPath;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * Extends {@link GwcUrlHandlerMapping} to fix virtual service URL handling when the workspace
 * prefix must be stripped before Spring's handler matching.
 *
 * <p>The upstream {@link GwcUrlHandlerMapping} strips the workspace prefix from the lookupPath
 * string and expects {@link UrlPathHelper#PATH_ATTRIBUTE} to be set on the request so its internal
 * {@code Wrapper} can adjust it accordingly. This subclass ensures both {@link
 * UrlPathHelper#PATH_ATTRIBUTE} (used by {@code AntPathMatcher}-based matching) and {@link
 * ServletRequestPathUtils#PATH_ATTRIBUTE} (used by {@code PathPattern}-based matching) are properly
 * set before delegating to the parent, and restores them afterwards.
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

        String strippedPath = lookupPath.substring(gwcRestBaseIndex);
        String contextPath = request.getContextPath();

        Object originalParsedPath = request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
        Object originalUrlPath = request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE);

        RequestPath parsedStrippedPath = RequestPath.parse(contextPath + strippedPath, contextPath);
        request.setAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE, parsedStrippedPath);
        request.setAttribute(UrlPathHelper.PATH_ATTRIBUTE, lookupPath);

        try {
            return super.lookupHandlerMethod(lookupPath, request);
        } finally {
            restoreAttribute(request, ServletRequestPathUtils.PATH_ATTRIBUTE, originalParsedPath);
            restoreAttribute(request, UrlPathHelper.PATH_ATTRIBUTE, originalUrlPath);
        }
    }

    private static void restoreAttribute(HttpServletRequest request, String name, Object original) {
        if (original != null) {
            request.setAttribute(name, original);
        } else {
            request.removeAttribute(name);
        }
    }
}
