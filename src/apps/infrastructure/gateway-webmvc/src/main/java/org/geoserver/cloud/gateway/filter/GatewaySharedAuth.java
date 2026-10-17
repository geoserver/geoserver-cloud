/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gateway.SharedAuthConfigurationProperties;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * SCG MVC filter that implements gateway shared authentication.
 *
 * <p>Merges the pre-filter and post-filter logic into a single {@link HandlerFilterFunction}, wrapping the proxy
 * exchange call to handle both request header manipulation (pre) and response header capture (post).
 *
 * <p>Pre-processing: strips incoming {@code x-gsc-username} and {@code x-gsc-roles} headers to prevent external
 * impersonation, then injects session-stored credentials for downstream services.
 *
 * <p>Post-processing: captures {@code x-gsc-username} and {@code x-gsc-roles} from the proxied response, updates the
 * session accordingly, and removes these headers from the final client response.
 *
 * <p>Example usage in YAML config:
 *
 * <pre>{@code
 * filters:
 *   - SharedAuth
 * }</pre>
 *
 * @since 3.0.0
 */
@Slf4j(topic = "org.geoserver.cloud.security.gateway.sharedauth")
class GatewaySharedAuth implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    static final String X_GSCLOUD_USERNAME = "x-gsc-username";
    static final String X_GSCLOUD_ROLES = "x-gsc-roles";

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        if (!isEnabled(request)) {
            return next.handle(request);
        }
        HttpServletRequest servletRequest = request.servletRequest();

        // PRE: strip incoming x-gsc-* headers (prevent impersonation),
        //      inject session-stored credentials for downstream
        ServerRequest modifiedRequest = preProcess(request);

        // Proxy exchange
        ServerResponse response = next.handle(modifiedRequest);

        // POST: capture x-gsc-* from response, update session, strip from response
        postProcess(servletRequest, response);
        return response;
    }

    private static boolean isEnabled(ServerRequest request) {
        return WebApplicationContextUtils.getRequiredWebApplicationContext(
                        request.servletRequest().getServletContext())
                .getBean(SharedAuthConfigurationProperties.class)
                .isEnabled();
    }

    /**
     * Strips incoming shared-auth headers to prevent impersonation and adds session-stored credentials for downstream
     * services.
     */
    private static ServerRequest preProcess(ServerRequest request) {
        HttpServletRequest servletRequest = request.servletRequest();
        logImpersonationAttempt(servletRequest);

        HttpSession session = servletRequest.getSession(false);
        String username = session != null ? (String) session.getAttribute(X_GSCLOUD_USERNAME) : null;
        @SuppressWarnings("unchecked")
        List<String> roles = session != null ? (List<String>) session.getAttribute(X_GSCLOUD_ROLES) : null;

        ServerRequest.Builder builder = ServerRequest.from(request);
        builder.headers(headers -> {
            headers.remove(X_GSCLOUD_USERNAME);
            headers.remove(X_GSCLOUD_ROLES);

            if (StringUtils.hasText(username)) {
                List<String> sessionRoles = roles != null ? roles : List.of();
                headers.set(X_GSCLOUD_USERNAME, username);
                sessionRoles.forEach(role -> headers.add(X_GSCLOUD_ROLES, role));
                if (log.isDebugEnabled()) {
                    log.debug(
                            "appended shared-auth request headers from session[{}] {}: {}, {}: {} to {} {}",
                            session.getId(),
                            X_GSCLOUD_USERNAME,
                            urlEncode(username),
                            X_GSCLOUD_ROLES,
                            concatRoleNames(sessionRoles),
                            sanitizeMethod(request.method()),
                            sanitizeUri(String.valueOf(request.uri())));
                }
            } else if (log.isTraceEnabled()) {
                log.trace(
                        "{} from session is '{}', not appending shared-auth headers to {} {}",
                        X_GSCLOUD_USERNAME,
                        urlEncode(username),
                        sanitizeMethod(request.method()),
                        sanitizeUri(String.valueOf(request.uri())));
            }
        });
        return builder.build();
    }

    /**
     * Captures shared-auth headers from the proxied response, updates the session, and removes them from the final
     * response.
     */
    private static void postProcess(HttpServletRequest servletRequest, ServerResponse response) {
        @Nullable List<String> responseUser = response.headers().remove(X_GSCLOUD_USERNAME);
        if (responseUser != null) {
            String username = responseUser.get(0);
            if (StringUtils.hasText(username)) {
                List<String> roles = response.headers().getOrDefault(X_GSCLOUD_ROLES, List.of());
                loggedIn(servletRequest, username, roles);
            } else {
                loggedOut(servletRequest);
            }
        }

        // Always remove shared-auth headers from the final response
        response.headers().remove(X_GSCLOUD_USERNAME);
        response.headers().remove(X_GSCLOUD_ROLES);
    }

    private static void loggedIn(HttpServletRequest request, @NonNull String user, List<String> roles) {
        HttpSession session = request.getSession(true);
        Object currUser = session.getAttribute(X_GSCLOUD_USERNAME);
        Object currRoles = session.getAttribute(X_GSCLOUD_ROLES);
        if (Objects.equals(user, currUser) && Objects.equals(roles, currRoles)) {
            if (log.isTraceEnabled()) {
                log.trace(
                        "user {} already present in session[{}], ignoring headers from {} {}",
                        urlEncode(user),
                        session.getId(),
                        sanitizeMethod(request.getMethod()),
                        sanitizeUri(request.getRequestURI()));
            }
            return;
        }
        session.setAttribute(X_GSCLOUD_USERNAME, user);
        session.setAttribute(X_GSCLOUD_ROLES, roles);
        if (log.isDebugEnabled()) {
            log.debug(
                    "stored shared-auth in session[{}], user '{}', roles '{}', as returned by {} {}",
                    session.getId(),
                    urlEncode(user),
                    concatRoleNames(roles),
                    sanitizeMethod(request.getMethod()),
                    sanitizeUri(request.getRequestURI()));
        }
    }

    private static void loggedOut(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(X_GSCLOUD_USERNAME) != null) {
            String user = (String) session.getAttribute(X_GSCLOUD_USERNAME);
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) session.getAttribute(X_GSCLOUD_ROLES);
            session.removeAttribute(X_GSCLOUD_USERNAME);
            session.removeAttribute(X_GSCLOUD_ROLES);
            if (log.isDebugEnabled()) {
                log.debug(
                        "removed shared-auth user {} roles {} from session[{}] as returned by {} {}",
                        urlEncode(user),
                        concatRoleNames(roles),
                        session.getId(),
                        sanitizeMethod(request.getMethod()),
                        sanitizeUri(request.getRequestURI()));
            }
        }
    }

    private static void logImpersonationAttempt(HttpServletRequest request) {
        if (request.getHeader(X_GSCLOUD_USERNAME) != null) {
            log.warn(
                    "removed incoming request header {}: {}. Request: [{} {}], from: {}",
                    urlEncode(X_GSCLOUD_USERNAME),
                    urlEncode(request.getHeader(X_GSCLOUD_USERNAME)),
                    sanitizeMethod(request.getMethod()),
                    sanitizeUri(request.getRequestURI()),
                    request.getRemoteAddr());
        }
        if (request.getHeader(X_GSCLOUD_ROLES) != null) {
            log.warn(
                    "removed incoming request header {}: {}. Request: [{} {}], from: {}",
                    X_GSCLOUD_ROLES,
                    concatRoleNames(request),
                    sanitizeMethod(request.getMethod()),
                    sanitizeUri(request.getRequestURI()),
                    request.getRemoteAddr());
        }
    }

    private static String concatRoleNames(HttpServletRequest request) {
        ArrayList<String> roles = Collections.list(request.getHeaders(X_GSCLOUD_ROLES));
        return concatRoleNames(roles);
    }

    private static String concatRoleNames(List<String> roles) {
        return roles.stream().map(GatewaySharedAuth::urlEncode).collect(Collectors.joining(","));
    }

    private static String sanitizeMethod(HttpMethod method) {
        return sanitizeMethod(String.valueOf(method));
    }

    static String sanitizeMethod(String method) {
        // validate the HTTP Method against an allow-list
        if (method == null || !method.matches("^[A-Z]+$")) {
            method = "INVALID_METHOD";
        }
        return method;
    }

    /**
     * Replaces the characters other than printable ASCII with an underscore, before the URI goes to the log. A line
     * break in the URI would split the log line.
     */
    static String sanitizeUri(String requestURI) {
        return (requestURI == null) ? "" : requestURI.replaceAll("[^\\x20-\\x7E]", "_");
    }

    @SneakyThrows
    static String urlEncode(String userInput) {
        return userInput == null ? null : URLEncoder.encode(userInput, "UTF-8");
    }
}
