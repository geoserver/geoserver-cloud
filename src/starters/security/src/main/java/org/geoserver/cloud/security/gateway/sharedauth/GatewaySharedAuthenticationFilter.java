/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
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
 * @since 1.9
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j(topic = "org.geoserver.cloud.security.gateway.sharedauth")
class GatewaySharedAuthenticationFilter extends GeoServerSecurityFilter implements GeoServerAuthenticationFilter {

    static final String X_GSCLOUD_USERNAME = "x-gsc-username";
    static final String X_GSCLOUD_ROLES = "x-gsc-roles";

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
        protected Collection<GeoServerRole> getRolesFromHttpAttribute(HttpServletRequest request, String principal) {

            GeoServerRoleConverter roleConverter = super.getConverter();

            var rolesHeader = getRolesHeaderAttribute();
            var rolesEnum = request.getHeaders(rolesHeader);
            return stream(rolesEnum.asIterator())
                    .filter(StringUtils::hasText)
                    .map(role -> roleConverter.convertRoleFromString(role, principal))
                    .toList();
        }
    }

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
        if (null == cookies || cookies.length == 0) return null;
        return Stream.of(cookies)
                .filter(c -> "SESSION".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /** No-op GeoServerSecurityFilter */
    static class DisabledFilter extends GeoServerSecurityFilter {

        @Override
        @SneakyThrows({ServletException.class, IOException.class})
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
            log.debug("gateway shared auth filter pass-through, functionality disabled");
            chain.doFilter(request, response);
        }
    }
}
