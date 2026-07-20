/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.filter;

import org.springframework.cloud.gateway.server.mvc.common.Shortcut;
import org.springframework.cloud.gateway.server.mvc.filter.FilterSupplier;
import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Single entry point for all custom GeoServer Cloud gateway filter functions.
 *
 * <p>Follows the Spring Cloud Gateway Server MVC convention established by
 * {@link org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions FilterFunctions}: an interface with
 * {@code static} methods annotated with {@link Shortcut @Shortcut}, and a single {@link GeoServerGatewayFilterSupplier}
 * that exposes all filters for YAML route configuration.
 *
 * @since 3.0.0
 */
public interface GeoServerGatewayFilterFunctions {

    /**
     * Strips a configurable base path prefix from the request URI before forwarding.
     *
     * <p>YAML: {@code filters: - StripBasePath=/geoserver/cloud}
     *
     * @see StripBasePath
     */
    @Shortcut
    static HandlerFilterFunction<ServerResponse, ServerResponse> stripBasePath(String prefix) {
        return new StripBasePath(prefix);
    }

    /**
     * No-op overload matched when the base path is empty. The shipped gateway config declares
     * {@code StripBasePath=${basepath}} on every route; with an empty base path the shortcut expands to
     * {@code StripBasePath=} with no arguments, and Spring Cloud Gateway Server MVC resolves shortcuts by matching
     * methods with the same argument count. There is no prefix to strip, requests pass through unchanged.
     *
     * @see StripBasePath
     */
    @Shortcut
    static HandlerFilterFunction<ServerResponse, ServerResponse> stripBasePath() {
        return (request, next) -> next.handle(request);
    }

    /**
     * Enables a route only if a given Spring profile is active.
     *
     * <p>YAML: {@code filters: - RouteProfile=dev,403}
     *
     * @see RouteProfile
     */
    @Shortcut
    static HandlerFilterFunction<ServerResponse, ServerResponse> routeProfile(String profile, int statusCode) {
        return new RouteProfile(profile, statusCode);
    }

    /**
     * Adds security-related HTTP response headers.
     *
     * <p>YAML: {@code filters: - SecureHeaders}
     *
     * @see SecureHeaders
     */
    @Shortcut
    static HandlerFilterFunction<ServerResponse, ServerResponse> secureHeaders() {
        return new SecureHeaders();
    }

    /**
     * Manages shared authentication between gateway and backend services via HTTP session.
     *
     * <p>YAML: {@code filters: - SharedAuth}
     *
     * @see GatewaySharedAuth
     */
    @Shortcut
    static HandlerFilterFunction<ServerResponse, ServerResponse> sharedAuth() {
        return new GatewaySharedAuth();
    }

    /** Single {@link FilterSupplier} that exposes all custom gateway filters for YAML config. */
    class GeoServerGatewayFilterSupplier extends SimpleFilterSupplier {

        public GeoServerGatewayFilterSupplier() {
            super(GeoServerGatewayFilterFunctions.class);
        }
    }
}
