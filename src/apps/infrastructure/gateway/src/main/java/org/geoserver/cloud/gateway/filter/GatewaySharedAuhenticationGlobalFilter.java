/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gateway.filter;

import org.springframework.boot.web.servlet.server.Session;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * {@link GlobalFilter} to enable sharing the webui form-based authentication object with the other
 * services.
 *
 * <p>When a user is logged in through the regular web ui's authentication form, the {@link
 * Authentication} object is held in the web ui's {@link Session}. Hence, further requests to
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
 * @since 1.9
 */
public class GatewaySharedAuhenticationGlobalFilter implements GlobalFilter {

    static final String X_GSCLOUD_USERNAME = "x-gsc-username";
    static final String X_GSCLOUD_ROLES = "x-gsc-roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // first, remove any incoming header to prevent impersonation
        exchange = removeRequestHeaders(exchange);

        return addHeadersFromSession(exchange)
                .flatMap(mutatedExchange -> proceed(mutatedExchange, chain))
                .flatMap(this::saveHeadersInSession)
                .flatMap(this::removeResponseHeaders);
    }

    private Mono<ServerWebExchange> proceed(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).thenReturn(exchange);
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
    private Mono<ServerWebExchange> saveHeadersInSession(ServerWebExchange exchange) {

        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        if (responseHeaders.containsKey(X_GSCLOUD_USERNAME)) {
            return exchange.getSession()
                    .flatMap(session -> save(responseHeaders, session))
                    .thenReturn(exchange);
        }

        return Mono.just(exchange);
    }

    private Mono<Void> save(HttpHeaders responseHeaders, WebSession session) {
        assert responseHeaders.containsKey(X_GSCLOUD_USERNAME);

        Optional<String> userame =
                responseHeaders.getOrDefault(X_GSCLOUD_USERNAME, List.of()).stream()
                        .filter(StringUtils::hasText)
                        .findFirst();

        var roles = responseHeaders.getOrDefault(X_GSCLOUD_ROLES, List.of());

        return Mono.fromRunnable(
                () ->
                        userame.ifPresentOrElse(
                                user -> {
                                    var attributes = session.getAttributes();
                                    attributes.put(X_GSCLOUD_USERNAME, user);
                                    attributes.put(X_GSCLOUD_ROLES, roles);
                                },
                                () -> {
                                    var attributes = session.getAttributes();
                                    attributes.remove(X_GSCLOUD_USERNAME);
                                    attributes.remove(X_GSCLOUD_ROLES);
                                }));
    }

    /**
     * Before proceeding with the filter chain, if the username and roles are stored in the session,
     * apply the request headers for the proxied service
     */
    private Mono<ServerWebExchange> addHeadersFromSession(ServerWebExchange exchange) {
        return exchange.getSession().map(session -> addHeadersFromSession(session, exchange));
    }

    private ServerWebExchange addHeadersFromSession(
            WebSession session, ServerWebExchange exchange) {

        String username = session.getAttributeOrDefault(X_GSCLOUD_USERNAME, "");
        if (StringUtils.hasText(username)) {
            String[] roles =
                    session.getAttributeOrDefault(X_GSCLOUD_ROLES, List.of())
                            .toArray(String[]::new);
            var request =
                    exchange.getRequest()
                            .mutate()
                            .header(X_GSCLOUD_USERNAME, username)
                            .header(X_GSCLOUD_ROLES, roles)
                            .build();
            exchange = exchange.mutate().request(request).build();
        }
        return exchange;
    }

    private ServerWebExchange removeRequestHeaders(ServerWebExchange exchange) {
        if (impersonating(exchange)) {
            var request = exchange.getRequest().mutate().headers(this::removeHeaders).build();
            exchange = exchange.mutate().request(request).build();
        }
        return exchange;
    }

    private Mono<Void> removeResponseHeaders(ServerWebExchange exchange) {
        return Mono.fromRunnable(
                () -> {
                    HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
                    removeHeaders(responseHeaders);
                });
    }

    private void removeHeaders(HttpHeaders httpHeaders) {
        httpHeaders.remove(X_GSCLOUD_USERNAME);
        httpHeaders.remove(X_GSCLOUD_ROLES);
    }

    private boolean impersonating(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        return headers.containsKey(X_GSCLOUD_USERNAME) || headers.containsKey(X_GSCLOUD_ROLES);
    }
}
