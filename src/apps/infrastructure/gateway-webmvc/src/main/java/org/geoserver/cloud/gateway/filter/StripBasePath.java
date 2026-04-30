/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.filter;

import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Custom gateway filter that strips a configurable base path prefix from the request URI before forwarding to
 * downstream services.
 *
 * <p>Works around the fact that Spring Cloud Gateway does not natively support a configurable base path for all routes.
 * The filter dynamically determines how many path segments to strip based on the configured prefix.
 *
 * <p>Example usage in YAML config:
 *
 * <pre>{@code
 * default-filters:
 *   - StripBasePath=/geoserver/cloud
 * }</pre>
 *
 * @see <a href="https://github.com/spring-cloud/spring-cloud-gateway/issues/1759">#1759</a>
 * @since 3.0.0
 */
class StripBasePath implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private final String prefix;

    public StripBasePath(String prefix) {
        checkPreconditions(prefix);
        this.prefix = prefix;
    }

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        String path = request.uri().getRawPath();
        int partsToRemove = resolvePartsToStrip(prefix, path);
        if (partsToRemove == 0) {
            return next.handle(request);
        }
        // Delegate to the built-in stripPrefix before-filter
        ServerRequest strippedRequest =
                BeforeFilterFunctions.stripPrefix(partsToRemove).apply(request);
        return next.handle(strippedRequest);
    }

    private static int resolvePartsToStrip(String basePath, String requestPath) {
        if (basePath == null || basePath.isEmpty() || !requestPath.startsWith(basePath)) {
            return 0;
        }
        int basePathSteps = StringUtils.countOccurrencesOf(basePath, "/");
        boolean isRoot = basePath.equals(requestPath);
        return isRoot ? basePathSteps - 1 : basePathSteps;
    }

    private static void checkPreconditions(String prefix) {
        if (prefix != null) {
            if (!prefix.startsWith("/")) {
                throw new IllegalStateException("StripBasePath prefix must start with '/', got '%s'".formatted(prefix));
            }
            if (!"/".equals(prefix) && prefix.endsWith("/")) {
                throw new IllegalStateException(
                        "StripBasePath prefix must not end with '/', got '%s'".formatted(prefix));
            }
        }
    }
}
