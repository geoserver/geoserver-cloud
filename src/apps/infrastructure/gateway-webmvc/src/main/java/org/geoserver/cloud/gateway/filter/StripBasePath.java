/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.filter;

import java.net.URI;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
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
 * <p>The stripped URI keeps the raw query string verbatim. Spring Cloud Gateway's own {@code stripPrefix} rebuilds the
 * URI through {@code UriComponentsBuilder.build(true)}, whose validation rejects a bare {@code =} inside a query value
 * ({@code CQL_FILTER=a=1}, {@code viewparams=a:b=c}) and turned such requests into a 500 response, although the
 * services accept them as sent.
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
        ServerRequest strippedRequest = stripRequestPath(request, partsToRemove);
        return next.handle(strippedRequest);
    }

    private static int resolvePartsToStrip(String basePath, String requestPath) {
        boolean emptyPrefix = basePath == null || basePath.isEmpty() || "/".equals(basePath);
        if (emptyPrefix || !requestPath.startsWith(basePath)) {
            return 0;
        }
        int basePathSteps = StringUtils.countOccurrencesOf(basePath, "/");
        boolean isRoot = basePath.equals(requestPath);
        return isRoot ? basePathSteps - 1 : basePathSteps;
    }

    /**
     * Same outcome as Spring Cloud Gateway's {@code BeforeFilterFunctions.stripPrefix(parts)}, the original URL
     * recorded for the {@code X-Forwarded-Prefix} header included, except that the query string is copied as received
     * instead of being parsed and validated again.
     */
    private static ServerRequest stripRequestPath(ServerRequest request, int parts) {
        URI uri = request.uri();
        MvcUtils.addOriginalRequestUrl(request, uri);
        String strippedPath = stripLeadingSegments(uri.getRawPath(), parts);
        URI strippedUri = withPath(uri, strippedPath);
        return ServerRequest.from(request).uri(strippedUri).build();
    }

    private static String stripLeadingSegments(String rawPath, int parts) {
        String[] segments = StringUtils.tokenizeToStringArray(rawPath, "/");
        StringBuilder stripped = new StringBuilder("/");
        for (int i = parts; i < segments.length; i++) {
            if (stripped.length() > 1) {
                stripped.append('/');
            }
            stripped.append(segments[i]);
        }
        if (stripped.length() > 1 && rawPath.endsWith("/")) {
            stripped.append('/');
        }
        return stripped.toString();
    }

    /** Replaces the path, keeping scheme, authority, query and fragment exactly as they were received. */
    private static URI withPath(URI uri, String rawPath) {
        StringBuilder rebuilt = new StringBuilder();
        if (uri.getScheme() != null) {
            rebuilt.append(uri.getScheme()).append(':');
        }
        if (uri.getRawAuthority() != null) {
            rebuilt.append("//").append(uri.getRawAuthority());
        }
        rebuilt.append(rawPath);
        if (uri.getRawQuery() != null) {
            rebuilt.append('?').append(uri.getRawQuery());
        }
        if (uri.getRawFragment() != null) {
            rebuilt.append('#').append(uri.getRawFragment());
        }
        return URI.create(rebuilt.toString());
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
