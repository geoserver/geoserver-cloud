/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.security.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.security.filter.GeoServerRequestHeaderAuthenticationFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * @since 1.2
 */
@Slf4j
@SuppressWarnings("java:S110") // not much to do about the large inheritance chain
class GatewayPreAuthenticationFilter extends GeoServerRequestHeaderAuthenticationFilter {

    /** Try to authenticate if there is no authenticated principal */
    @Override
    public void doFilter(final ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        String cacheKey = authenticateFromCache(this, httpRequest);

        String principalName = getPreAuthenticatedPrincipalName(httpRequest);

        Authentication preAuth = SecurityContextHolder.getContext().getAuthentication();

        // If a pre-auth token exists but the request has no principal name anymore, clear the
        // context, or the user will stay authenticated
        if (preAuth instanceof PreAuthenticatedAuthenticationToken && null == principalName) {
            SecurityContextHolder.clearContext();
            preAuth = null;
        }

        if (preAuth == null || principalName != null) {
            log.debug("Authenticating as {}", principalName);
            doAuthenticate(httpRequest, (HttpServletResponse) response);

            Authentication postAuthentication =
                    SecurityContextHolder.getContext().getAuthentication();

            boolean authenticated = postAuthentication != null;
            boolean cached = cacheKey != null;
            boolean sessionAvailable = cacheAuthentication(postAuthentication, httpRequest);
            if (authenticated && cached && sessionAvailable) {
                getSecurityManager().getAuthenticationCache().put(getName(), cacheKey, postAuthentication);
            }
        }

        request.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        chain.doFilter(request, response);
    }
}
