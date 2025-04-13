/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.security.gateway.sharedauth;

import static org.geoserver.cloud.security.gateway.sharedauth.SharedAuthConfigurationProperties.X_GSCLOUD_ROLES;
import static org.geoserver.cloud.security.gateway.sharedauth.SharedAuthConfigurationProperties.X_GSCLOUD_USERNAME;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Authentication;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * {@link GlobalFilter} working in tandem with {@link GatewaySharedAuthenticationPostFilter} to
 * enable sharing the webui form-based authentication object with the other services.
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
 * <p>This is the pre-filter in the above mentioned workflow, taking care of avoiding external
 * impresonation attempts by removing the {@literal x-gsc-username} and {@literal x-gsc-roles}
 * headers from incoming requests, and appending them to proxied requests using the values taken
 * from the {@link WebSession}, if present (as stored by the {@link
 * GatewaySharedAuthenticationPostFilter post-filter}.
 *
 * @since 1.9
 * @see GatewaySharedAuthenticationPostFilter
 */
@Slf4j(topic = "org.geoserver.cloud.security.gateway.sharedauth.pre")
public class GatewaySharedAuthenticationPreFilter implements GlobalFilter, Ordered {

    /**
     * @return {@link Ordered#HIGHEST_PRECEDENCE}, being a pre-filter, means it'll run the first for
     *     pre-processing before the request executed
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // first, remove any incoming header to prevent impersonation
        exchange = removeRequestHeaders(exchange);
        return addHeadersFromSession(exchange).flatMap(chain::filter).then();
    }

    /**
     * Before proceeding with the filter chain, if the username and roles are stored in the session,
     * apply the request headers for the proxied service
     */
    private Mono<ServerWebExchange> addHeadersFromSession(ServerWebExchange exchange) {
        return exchange.getSession().map(session -> addHeadersFromSession(session, exchange));
    }

    private ServerWebExchange addHeadersFromSession(WebSession session, ServerWebExchange exchange) {
        final String username = session.getAttribute(X_GSCLOUD_USERNAME);
        if (StringUtils.hasText(username)) {
            final List<String> roles = session.getAttributeOrDefault(X_GSCLOUD_ROLES, List.of());
            final var origRequest = exchange.getRequest();
            var request = origRequest
                    .mutate()
                    .headers(headers -> {
                        headers.set(X_GSCLOUD_USERNAME, username);
                        headers.remove(X_GSCLOUD_ROLES);
                        roles.forEach(role -> headers.add(X_GSCLOUD_ROLES, role));
                        log.debug(
                                "appended shared-auth request headers from session[{}] {}: {}, {}: {} to {} {}",
                                session.getId(),
                                X_GSCLOUD_USERNAME,
                                username,
                                X_GSCLOUD_ROLES,
                                roles,
                                origRequest.getMethod(),
                                origRequest.getURI().getPath());
                    })
                    .build();

            exchange = exchange.mutate().request(request).build();
        } else {
            log.trace(
                    "{} from session[{}] is '{}', not appending shared-auth headers to {} {}",
                    X_GSCLOUD_USERNAME,
                    session.getId(),
                    username,
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getPath());
        }
        return exchange;
    }

    private ServerWebExchange removeRequestHeaders(ServerWebExchange exchange) {
        if (impersonationAttempt(exchange)) {
            var origRequest = exchange.getRequest();
            var request = exchange.getRequest()
                    .mutate()
                    .headers(headers -> removeRequestHeaders(origRequest, headers))
                    .build();
            exchange = exchange.mutate().request(request).build();
        }
        return exchange;
    }

    private void removeRequestHeaders(ServerHttpRequest origRequest, HttpHeaders requestHeaders) {
        removeRequestHeader(origRequest, requestHeaders, X_GSCLOUD_USERNAME);
        removeRequestHeader(origRequest, requestHeaders, X_GSCLOUD_ROLES);
    }

    private void removeRequestHeader(ServerHttpRequest origRequest, HttpHeaders headers, String name) {
        removeHeader(headers, name).ifPresent(value -> {
            HttpMethod method = origRequest.getMethod();
            URI uri = origRequest.getURI();
            InetSocketAddress remoteAddress = origRequest.getRemoteAddress();
            log.warn(
                    "removed incoming request header {}: {}. Request: [{} {}], from: {}",
                    name,
                    value,
                    method,
                    uri,
                    remoteAddress);
        });
    }

    private Optional<String> removeHeader(HttpHeaders httpHeaders, String name) {
        List<String> values = httpHeaders.remove(name);
        return Optional.ofNullable(values).map(l -> l.stream().collect(Collectors.joining(",")));
    }

    private boolean impersonationAttempt(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        return headers.containsKey(X_GSCLOUD_USERNAME) || headers.containsKey(X_GSCLOUD_ROLES);
    }
}
