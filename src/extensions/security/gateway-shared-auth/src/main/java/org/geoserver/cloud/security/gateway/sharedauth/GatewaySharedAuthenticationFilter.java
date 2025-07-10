/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.security.gateway.sharedauth;

import static com.google.common.collect.Streams.stream;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerRequestHeaderAuthenticationFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerRoleConverterImpl;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

/**
 * Authentication filter that enables shared authentication across GeoServer Cloud microservices.
 *
 * <p>This filter provides two operational modes:
 * <ul>
 *   <li><b>Server mode</b>: Used in the WebUI service to add authentication headers to responses
 *       when a user is authenticated. These headers contain the username and roles.
 *   <li><b>Client mode</b>: Used in all other services to process authentication headers from
 *       incoming requests that have been forwarded by the gateway.
 * </ul>
 *
 * <p>The filter relies on the API Gateway to act as an intermediary, storing the authentication
 * information from the WebUI service responses and adding it to requests to other services.
 *
 * <p><strong>IMPORTANT:</strong> The package name must not be changed as it's used in XStream
 * serialization of security configuration.
 *
 * @see GatewaySharedAuthenticationProvider
 * @since 1.9
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j(topic = "org.geoserver.cloud.security.gateway.sharedauth")
public class GatewaySharedAuthenticationFilter extends GeoServerSecurityFilter
        implements GeoServerAuthenticationFilter {

    static final String X_GSCLOUD_USERNAME = "x-gsc-username";
    static final String X_GSCLOUD_ROLES = "x-gsc-roles";

    /**
     * Configuration class for the {@link GatewaySharedAuthenticationFilter}.
     *
     * <p>This class is serialized by GeoServer's security subsystem using XStream, so its
     * package name and class name must remain stable to maintain backward compatibility.</p>
     *
     * <p>The filter configuration is simple and doesn't require any custom properties
     * as the operational mode (server/client/disabled) is determined at runtime by the
     * Spring configuration.</p>
     */
    @SuppressWarnings("serial")
    public static class Config extends SecurityFilterConfig implements SecurityAuthFilterConfig {
        public Config() {
            super.setClassName(GatewaySharedAuthenticationFilter.class.getCanonicalName());
            super.setName("gateway-shared-auth");
        }
    }

    private final @NonNull GeoServerSecurityFilter delegate;

    /**
     * @return a {@link GatewaySharedAuthenticationFilter} proxy filter with a {@link ServerFilter}
     *     delegate
     */
    public static GeoServerSecurityFilter server() {
        return new GatewaySharedAuthenticationFilter(new ServerFilter());
    }

    /**
     * @return a {@link GatewaySharedAuthenticationFilter} proxy filter with a {@link ClientFilter}
     *     delegate
     */
    public static GeoServerSecurityFilter client() {
        return new GatewaySharedAuthenticationFilter(new ClientFilter());
    }

    /**
     * @return a {@link GatewaySharedAuthenticationFilter} proxy filter with a
     *     <strong>no-op</strong> {@link DisabledFilter} delegate. This prevents startup failures
     *     and WebUI security settings editing failures, when the filter has been disabled through
     *     {@code geoserver.security.gateway-shared-auth.enabled=false} after it's been enabled.
     */
    public static GeoServerSecurityFilter disabled() {
        return new GatewaySharedAuthenticationFilter(new DisabledFilter());
    }

    @Override
    public boolean applicableForHtml() {
        return true;
    }

    @Override
    public boolean applicableForServices() {
        return true;
    }

    @Override
    @SneakyThrows({ServletException.class, IOException.class})
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {

        delegate.doFilter(request, response, chain);
    }

    /**
     * Client-side implementation of the Gateway Shared Authentication filter.
     *
     * <p>This implementation extends {@link GeoServerRequestHeaderAuthenticationFilter} to process
     * authentication headers from incoming requests. It extracts the username from the
     * {@link #X_GSCLOUD_USERNAME} header and roles from the {@link #X_GSCLOUD_ROLES} headers
     * to authenticate the user.</p>
     *
     * <p>This filter is used in all services except the WebUI to receive authentication
     * information that was originally set by the WebUI and forwarded by the API Gateway.</p>
     */
    @SuppressWarnings("java:S110")
    static class ClientFilter extends GeoServerRequestHeaderAuthenticationFilter {

        ClientFilter() {
            super.setRoleSource(PreAuthenticatedUserNameRoleSource.Header);
            super.setConverter(new GeoServerRoleConverterImpl());
            super.setPrincipalHeaderAttribute(X_GSCLOUD_USERNAME);
            super.setRolesHeaderAttribute(X_GSCLOUD_ROLES);
            super.setRoleConverterName("");
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            var pre = SecurityContextHolder.getContext().getAuthentication();
            try {
                super.doFilter(request, response, chain);
            } finally {
                if (log.isDebugEnabled()) {
                    var post = SecurityContextHolder.getContext().getAuthentication();
                    HttpServletRequest req = (HttpServletRequest) request;
                    String preUsername = pre == null ? null : pre.getName();
                    String postUsername = post == null ? null : post.getName();
                    String reqHeaders = getHeaders(req);
                    String gatewaySessionId = getGatewaySessionId(req);
                    log.debug(
                            "[gateway session: {}] {} {}\n user pre: {}\n user post: {}\n headers: \n{}",
                            gatewaySessionId,
                            req.getMethod(),
                            req.getRequestURI(),
                            preUsername,
                            postUsername,
                            reqHeaders);
                }
            }
        }

        private String getHeaders(HttpServletRequest req) {
            return Streams.stream(req.getHeaderNames().asIterator())
                    .filter(h -> h.toLowerCase().startsWith("x-gsc"))
                    .map(name -> "\t%s: %s".formatted(name, req.getHeader(name)))
                    .collect(Collectors.joining("\n"));
        }

        /**
         * Override to handle multi-valued roles header, the super-class assumes a single-valued
         * header with a delimiter to handle multiple values
         */
        @Override
        protected Collection<GeoServerRole> getRoles(HttpServletRequest request, String principal) throws IOException {
            GeoServerRoleConverter roleConverter = super.getConverter();

            var rolesHeader = getRolesHeaderAttribute();
            var rolesEnum = request.getHeaders(rolesHeader);
            return stream(rolesEnum.asIterator())
                    .filter(StringUtils::hasText)
                    .map(role -> roleConverter.convertRoleFromString(role, principal))
                    .toList();
        }
    }

    /**
     * Server-side implementation of the Gateway Shared Authentication filter.
     *
     * <p>This implementation is used in the WebUI service to add authentication headers to responses
     * when a user is authenticated. It extracts the username and roles from the current
     * {@link SecurityContextHolder} and adds them as response headers.</p>
     *
     * <p>These headers are then captured by the API Gateway and associated with the user's session.
     * When the same user makes requests to other services, the gateway adds these headers to the
     * forwarded requests, enabling the client-side filter to authenticate the user.</p>
     */
    static class ServerFilter extends GeoServerSecurityFilter {

        @Override
        @SneakyThrows({ServletException.class, IOException.class})
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {

            // if the user is authenticated by some method, set the response headers for the
            // Gateway to act as the middle man and send the user and roles to the other
            // services
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            HttpServletResponse resp = (HttpServletResponse) response;
            HttpServletRequest req = (HttpServletRequest) request;

            if (auth != null && !(auth instanceof AnonymousAuthenticationToken) && auth.isAuthenticated()) {
                setGatewayResponseHeaders(auth, req, resp);
            } else if (auth == null || auth instanceof AnonymousAuthenticationToken) {
                setEmptyUserResponseHeader(req, resp);
            }

            chain.doFilter(request, response);
        }

        /**
         * Sets the {@link #X_GSCLOUD_USERNAME} request header to the empty string, the gateway
         * requires for it to be explicitly set to clear it out from its session, just removing the
         * header wouldn't work.
         */
        private void setEmptyUserResponseHeader(HttpServletRequest req, HttpServletResponse response) {
            response.setHeader(X_GSCLOUD_USERNAME, "");
            if (log.isDebugEnabled()) {
                String gatewaySessionId = getGatewaySessionId(req);
                log.debug(
                        "[gateway session: {}] sending empty {} response header for {} {}",
                        gatewaySessionId,
                        X_GSCLOUD_USERNAME,
                        req.getMethod(),
                        req.getRequestURI());
            }
        }

        private void setGatewayResponseHeaders(
                Authentication preAuth, HttpServletRequest req, HttpServletResponse response) {
            final String username = preAuth.getName();
            if (null != username) {
                response.setHeader(X_GSCLOUD_USERNAME, username);
                preAuth.getAuthorities()
                        .forEach(authority -> response.addHeader(X_GSCLOUD_ROLES, authority.getAuthority()));
                if (log.isDebugEnabled()) {
                    String gatewaySessionId = getGatewaySessionId(req);
                    log.debug(
                            "[gateway session: {}] appended response headers {}: {}, {}: {} for {} {}",
                            gatewaySessionId,
                            X_GSCLOUD_USERNAME,
                            response.getHeader(X_GSCLOUD_USERNAME),
                            X_GSCLOUD_ROLES,
                            response.getHeaders(X_GSCLOUD_ROLES),
                            req.getMethod(),
                            req.getRequestURI());
                }
            }
        }
    }

    static String getGatewaySessionId(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (null == cookies || cookies.length == 0) {
            return null;
        }
        return Stream.of(cookies)
                .filter(c -> "SESSION".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * No-op implementation of the Gateway Shared Authentication filter when the feature is disabled.
     *
     * <p>This implementation simply passes the request through to the next filter in the chain
     * without performing any authentication processing. It's used when the Gateway Shared
     * Authentication feature is explicitly disabled through configuration.</p>
     *
     * <p>Having this disabled implementation is important for backward compatibility when the filter
     * has been previously enabled and then disabled. It prevents startup failures and WebUI security
     * settings editing failures by providing a valid but inactive filter implementation.</p>
     */
    static class DisabledFilter extends GeoServerSecurityFilter {

        @Override
        @SneakyThrows({ServletException.class, IOException.class})
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
            log.debug("gateway shared auth filter pass-through, functionality disabled");
            chain.doFilter(request, response);
        }
    }
}
