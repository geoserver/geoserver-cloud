/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.web.core;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * A servlet filter that intercepts requests to Wicket bookmarkable pages and
 * returns a 404 (Not Found) response when the requested page belongs to a
 * component that has been disabled through configuration.
 *
 * <p>
 * This filter examines incoming requests to {@code /web/wicket/bookmarkable/*}
 * URLs, extracts the target page class name, and uses
 * {@link WebUIConfigurationProperties} to determine if access to that page
 * should be allowed based on the current configuration.
 * </p>
 *
 * <p>
 * Examples:
 * </p>
 * <ul>
 * <li>If {@code geoserver.web-ui.wms=false}, accessing
 * {@code /web/wicket/bookmarkable/org.geoserver.wms.web.WMSAdminPage} will
 * result in a 404 response</li>
 * <li>If {@code geoserver.web-ui.demos.layer-preview-page=false}, accessing
 * {@code /web/wicket/bookmarkable/org.geoserver.web.demo.LayerPreviewPage} will
 * result in a 404 response</li>
 * </ul>
 *
 * <p>
 * This filter allows site administrators to selectively disable UI components
 * while maintaining a consistent user experience by hiding those components
 * completely, rather than showing them but having them fail when accessed.
 * </p>
 */
@RequiredArgsConstructor
@NoArgsConstructor // For bean instantiation when registered manually
public class WicketComponentFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WicketComponentFilter.class);

    /**
     * Pattern to match bookmarkable page URLs. Handles both path formats:
     * {@literal /web/wicket/bookmarkable/org.geoserver.wms.web.WMSAdminPage} or
     * {@literal /geoserver/cloud/web/bookmarkable/org.geoserver.wms.web.WMSAdminPage}
     */
    private static final Pattern BOOKMARKABLE_URL_PATTERN =
            Pattern.compile(".*/(?:web/wicket/bookmarkable|web/bookmarkable)/([^?/]+)(?:\\?.*)?$");

    private @NonNull WebUIConfigurationProperties config;

    /**
     * Filters incoming HTTP requests to intercept and handle requests to disabled
     * Wicket components.
     *
     * <p>
     * The filter performs the following steps:
     * </p>
     * <ol>
     * <li>Checks if the request is an HTTP request and if configuration is
     * available</li>
     * <li>For Wicket bookmarkable page requests, determines if the requested page
     * belongs to a disabled component</li>
     * <li>If the page should be hidden (belongs to a disabled component):
     * <ul>
     * <li>Returns an HTTP 404 (Not Found) response</li>
     * <li>Sets the content type to text/plain</li>
     * <li>Writes a simple "Page not found" message to the response</li>
     * <li>Logs the blocked access at debug level</li>
     * </ul>
     * </li>
     * <li>Otherwise, passes the request to the next filter in the chain</li>
     * </ol>
     *
     * <p>
     * This method ensures that disabled components are completely hidden from
     * users, providing a consistent experience by returning the same status code
     * that would be returned for any other non-existent resource.
     * </p>
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || config == null) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (shouldHide(httpRequest)) {
            // Return a 404 Not Found response
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpStatus.NOT_FOUND.value());
            httpResponse.setContentType("text/plain");
            httpResponse.getWriter().println("Page not found");
            LOGGER.debug("Blocked access to disabled component: {}", httpRequest.getRequestURI());
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean shouldHide(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String bookmarkablePageClassName = extractBookmarkablePageClassName(requestURI);
        if (bookmarkablePageClassName != null) {
            // Return true if the page should be hidden (when enablePageClassUrl returns
            // false)
            return !config.enablePageClassUrl(bookmarkablePageClassName);
        }
        // If not a bookmarkable page URL, don't hide it
        return false;
    }

    /**
     * Extracts the wicket bookmarkable page class name (e.g.
     * {@code org.geoserver.wms.web.WMSAdminPage} from
     * {@code /web/wicket/bookmarkable/org.geoserver.wms.web.WMSAdminPage?filter=false})
     *
     * @param requestURI the request URI
     * @return the bookmarkable page class name, or null if the URI doesn't match
     *         the pattern
     */
    private String extractBookmarkablePageClassName(String requestURI) {
        if (requestURI != null) {
            Matcher matcher = BOOKMARKABLE_URL_PATTERN.matcher(requestURI);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
