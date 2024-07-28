/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security.gateway.sharedauth;

import static org.geoserver.cloud.security.gateway.sharedauth.SharedAuthConfigurationProperties.X_GSCLOUD_ROLES;
import static org.geoserver.cloud.security.gateway.sharedauth.SharedAuthConfigurationProperties.X_GSCLOUD_USERNAME;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Authentication;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link GlobalFilter} working in tandem with {@link GatewaySharedAuhenticationPreFilter} to enable
 * sharing the webui form-based authentication object with the other services.
 *
 * <p>When a user is logged in through the regular web ui's authentication form, the {@link
 * Authentication} object is held in the web ui's {@link WebSession}. Hence, further requests to
 * stateless services, as they're on separate containers, don't share the webui session, and hence
 * are executed as anonymous.
 *
 * <p>This {@link GlobalFilter} enables a mechanism by which the authenticated user name and roles
 * can be shared with the stateless services through request and response headrers, using the
 * geoserver cloud gateway as the man in the middle.
 *
 * <p>The webui container will send a couple response headers with the authenticated user name and
 * roles. The gateway will store them in its own session, and forward them to all services as
 * request headers. The stateless services will intercept these request headers and impersonate the
 * authenticated user as a {@link PreAuthenticatedAuthenticationToken}.
 *
 * <p>At the same time, the gateway will take care of removing the webui response headers from the
 * responses sent to the clients, and from incoming client requests.
 *
 * <p>This is the post-filter in the above mentioned workflow, and takes care of updating the {@link
 * WebSession} to either store or remove the {@literal x-gsc-username} and {@literal x-gsc-roles}
 * webui response headers, or clear them out from the session when the webui responds with an empty
 * string for {@literal x-gsc-username}.
 *
 * @since 1.9
 * @see GatewaySharedAuhenticationPreFilter
 */
@Slf4j(topic = "org.geoserver.cloud.security.gateway.sharedauth.post")
public class GatewaySharedAuhenticationPostFilter implements GlobalFilter, Ordered {

    /**
     * @return {@link Ordered#LOWEST_PRECEDENCE}, being a post-filter, means it'll run the first
     *     post-processing once the response is obtained from the downstream service
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .then(updateHeadersInSession(exchange))
                .then(removeResponseHeaders(exchange));
    }

    /**
     * After the filter chain's run, if the proxied service replied with the user and roles headers,
     * save them in the session.
     *
     * <p>
     *
     * <ul>
     *   <li>A missing request header does not change the session
     *   <li>An empty username header clears out the values in the session (i.e. it's a logout)
     * </ul>
     */
    private Mono<Void> updateHeadersInSession(ServerWebExchange exchange) {

        return exchange.getSession()
                .flatMap(
                        session ->
                                updateSession(
                                        exchange.getRequest(),
                                        exchange.getResponse().getHeaders(),
                                        session));
    }

    private Mono<Void> updateSession(
            ServerHttpRequest req, HttpHeaders responseHeaders, WebSession session) {
        final var responseUser = responseHeaders.getFirst(X_GSCLOUD_USERNAME);
        if (null == responseUser) {
            return Mono.empty();
        }

        final boolean isLogout = !StringUtils.hasText(responseUser);
        if (isLogout) {
            return Mono.fromRunnable(() -> loggedOut(session, req));
        }
        var roles = responseHeaders.getOrDefault(X_GSCLOUD_ROLES, List.of());
        return Mono.fromRunnable(() -> loggedIn(session, responseUser, roles, req));
    }

    private void loggedIn(
            WebSession session, String user, List<String> roles, ServerHttpRequest req) {
        var attributes = session.getAttributes();
        var currUser = attributes.get(X_GSCLOUD_USERNAME);
        var currRoles = attributes.get(X_GSCLOUD_ROLES);
        if (Objects.equals(user, currUser) && Objects.equals(roles, currRoles)) {
            log.trace(
                    "user {} already present in session[{}], ignoring headers from {} {}",
                    user,
                    session.getId(),
                    req.getMethod(),
                    req.getURI().getPath());
            return;
        }
        if (!session.isStarted()) {
            session.start();
        }
        attributes.put(X_GSCLOUD_USERNAME, user);
        attributes.put(X_GSCLOUD_ROLES, roles);
        log.debug(
                "stored shared-auth in session[{}], user '{}', roles '{}', as returned by {} {}",
                session.getId(),
                user,
                roles,
                req.getMethod(),
                req.getURI().getPath());
    }

    private void loggedOut(WebSession session, ServerHttpRequest req) {
        var attributes = session.getAttributes();
        if (session.isStarted() && attributes.containsKey(X_GSCLOUD_USERNAME)) {
            var user = attributes.remove(X_GSCLOUD_USERNAME);
            var roles = attributes.remove(X_GSCLOUD_ROLES);
            log.debug(
                    "removed shared-auth user {} roles {} from session[{}] as returned by {} {}",
                    user,
                    roles,
                    session.getId(),
                    req.getMethod(),
                    req.getURI().getPath());
        }
    }

    private Mono<Void> removeResponseHeaders(ServerWebExchange exchange) {
        return Mono.fromRunnable(
                () -> {
                    HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
                    removeResponseHeaders(exchange.getRequest(), responseHeaders);
                });
    }

    private void removeResponseHeaders(ServerHttpRequest req, HttpHeaders responseHeaders) {
        removeResponseHeader(req, responseHeaders, X_GSCLOUD_USERNAME);
        removeResponseHeader(req, responseHeaders, X_GSCLOUD_ROLES);
    }

    private void removeResponseHeader(ServerHttpRequest req, HttpHeaders headers, String name) {
        removeHeader(headers, name)
                .ifPresent(
                        value ->
                                log.trace(
                                        "removed response header {}: '{}'. {} {}",
                                        name,
                                        value,
                                        req.getMethod(),
                                        req.getURI().getPath()));
    }

    private Optional<String> removeHeader(HttpHeaders httpHeaders, String name) {
        List<String> values = httpHeaders.remove(name);
        return Optional.ofNullable(values).map(l -> l.stream().collect(Collectors.joining(",")));
    }
}
