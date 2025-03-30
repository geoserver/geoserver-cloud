/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.mdc.config;

import com.github.f4b6a3.ulid.UlidCreator;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NonNull;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

/**
 * Configuration properties for controlling which HTTP request information is included in the MDC.
 * <p>
 * These properties determine what request-related information is added to the MDC (Mapped Diagnostic Context)
 * during request processing. Including this information in the MDC makes it available to all logging
 * statements, providing valuable context for debugging, monitoring, and audit purposes.
 * <p>
 * The properties are configured using the prefix {@code logging.mdc.include.http} in the application
 * properties or YAML files.
 * <p>
 * Example configuration in YAML:
 * <pre>
 * logging:
 *   mdc:
 *     include:
 *       http:
 *         id: true
 *         method: true
 *         url: true
 *         remote-addr: true
 *         headers: true
 *         headers-pattern: "(?i)x-.*|correlation-.*"
 * </pre>
 * <p>
 * This class provides methods to extract and add HTTP request properties to the MDC based on the
 * configuration. It supports both Servlet and WebFlux environments through its flexible API.
 *
 * @see org.geoserver.cloud.logging.mdc.servlet.HttpRequestMdcFilter
 * @see org.geoserver.cloud.logging.mdc.webflux.MDCWebFilter
 */
@Data
@ConfigurationProperties(prefix = "logging.mdc.include.http")
public class HttpRequestMdcConfigProperties {

    public static final String REQUEST_ID_HEADER = "http.request.id";

    /**
     * Whether to append the http.request.id MDC property. The value is the id provided by the
     * http.request.id header, or a new monotonically increating UID if no such header is present.
     */
    private boolean id = true;

    /**
     * Whether to append the http.request.remote-addr MDC property, interpreted as the Internet
     * Protocol (IP) address of the client or last proxy that sent the request. For HTTP servlets,
     * same as the value of the CGI variable REMOTE_ADDR.
     */
    private boolean remoteAddr = false;

    /**
     * Whether to append the http.request.remote-host MDC property, interpreted as the fully
     * qualified name of the client or the last proxy that sent the request. If the engine cannot or
     * chooses not to resolve the hostname (to improve performance), this method returns the
     * dotted-string form of the IP address. For HTTP servlets, same as the value of the CGI
     * variable REMOTE_HOST. Defaults to false to avoid the possible overhead in reverse DNS
     * lookups. remoteAddress should be enough in most cases.
     */
    private boolean remoteHost = false;

    /** Whether to append the http.request.method MDC property */
    private boolean method = true;

    /** Whether to append the http.request.url MDC property, without the query string */
    private boolean url = true;

    /**
     * Whether to append one http.request.parameter.[name] MDC property from each request parameter
     */
    private boolean parameters = false;

    /**
     * Whether to append the http.request.query-string MDC property from the HTTP request query
     * string
     */
    private boolean queryString = false;

    /**
     * Whether to append the http.request.session.is MDC property if there's an HttpSession
     * associated to the request
     */
    private boolean sessionId = false;

    /** Whether to append one http.request.cookie.[name] MDC property from each request cookie */
    private boolean cookies = false;

    /**
     * Whether to append one http.request.header.[name] MDC property from each HTTP request header
     * whose name matches the headers-pattern
     */
    private boolean headers = false;

    /**
     * Java regular expression indicating which request header names to include when
     * logging.mdc.include.http.headers=true. Defaults to include all headers with the pattern '.*'
     */
    private Pattern headersPattern = Pattern.compile(".*");

    /**
     * Adds HTTP headers to the MDC if enabled by configuration.
     * <p>
     * This method extracts headers from the supplied HttpHeaders and adds them to the MDC
     * if {@link #isHeaders()} is true. Only headers matching the {@link #getHeadersPattern()}
     * will be included.
     *
     * @param headers a supplier that provides the HTTP headers
     * @return this instance for method chaining
     */
    public HttpRequestMdcConfigProperties headers(Supplier<HttpHeaders> headers) {
        if (isHeaders()) {
            HttpHeaders httpHeaders = headers.get();
            httpHeaders.forEach(this::putHeader);
        }
        return this;
    }

    /**
     * Adds HTTP cookies to the MDC if enabled by configuration.
     * <p>
     * This method extracts cookies from the supplied MultiValueMap and adds them to the MDC
     * if {@link #isCookies()} is true. Each cookie is added with the key format
     * {@code http.request.cookie.[name]}.
     *
     * @param cookies a supplier that provides the HTTP cookies
     * @return this instance for method chaining
     */
    public HttpRequestMdcConfigProperties cookies(Supplier<MultiValueMap<String, HttpCookie>> cookies) {
        if (isCookies()) {
            cookies.get().values().forEach(this::putCookie);
        }
        return this;
    }

    /**
     * Adds a list of cookies with the same name to the MDC.
     * <p>
     * This method processes a list of cookies and adds them to the MDC with the key format
     * {@code http.request.cookie.[name]}. If multiple cookies with the same name exist,
     * their values are concatenated with semicolons.
     *
     * @param cookies the list of cookies to add to the MDC
     */
    private void putCookie(List<HttpCookie> cookies) {
        cookies.forEach(c -> {
            String key = "http.request.cookie.%s".formatted(c.getName());
            String value = MDC.get(key);
            if (value == null) {
                value = c.getValue();
            } else {
                value = "%s;%s".formatted(value, c.getValue());
            }
            MDC.put(key, value);
        });
    }

    /**
     * Determines if a header should be included in the MDC based on the header pattern.
     * <p>
     * This method checks if the header name matches the pattern defined in {@link #getHeadersPattern()}.
     * The "cookie" header is always excluded because cookies are handled separately by
     * the {@link #cookies(Supplier)} method.
     *
     * @param headerName the name of the header to check
     * @return true if the header should be included, false otherwise
     */
    private boolean includeHeader(String headerName) {
        if ("cookie".equalsIgnoreCase(headerName)) return false;
        return getHeadersPattern().matcher(headerName).matches();
    }

    /**
     * Adds a header to the MDC if it matches the inclusion criteria.
     * <p>
     * This method adds a header to the MDC with the key format {@code http.request.header.[name]}
     * if the header name matches the inclusion pattern. Multiple values for the same header
     * are joined with commas.
     *
     * @param name the header name
     * @param values the list of header values
     */
    private void putHeader(String name, List<String> values) {
        if (includeHeader(name)) {
            put("http.request.header.%s".formatted(name), () -> values.stream().collect(Collectors.joining(",")));
        }
    }

    public HttpRequestMdcConfigProperties id(Supplier<HttpHeaders> headers) {
        put(REQUEST_ID_HEADER, this::isId, () -> findOrCreateRequestId(headers));
        return this;
    }

    public HttpRequestMdcConfigProperties method(Supplier<String> method) {
        put("http.request.method", this::isMethod, method);
        return this;
    }

    public HttpRequestMdcConfigProperties url(Supplier<String> url) {
        put("http.request.url", this::isUrl, url);
        return this;
    }

    public HttpRequestMdcConfigProperties queryString(Supplier<String> getQueryString) {
        put("http.request.query-string", this::isQueryString, getQueryString);
        return this;
    }

    public HttpRequestMdcConfigProperties parameters(Supplier<MultiValueMap<String, String>> parameters) {
        if (isParameters()) {
            Map<String, List<String>> params = parameters.get();
            params.forEach((k, v) -> put("http.request.parameter.%s".formatted(k), values(v)));
        }
        return this;
    }

    private Supplier<?> values(List<String> v) {
        return () -> null == v ? "" : v.stream().collect(Collectors.joining(","));
    }

    public HttpRequestMdcConfigProperties sessionId(Supplier<String> sessionId) {
        put("http.request.session.id", this::isSessionId, sessionId);
        return this;
    }

    public HttpRequestMdcConfigProperties remoteAddr(InetSocketAddress remoteAddr) {
        if (remoteAddr == null || !isRemoteAddr()) return this;
        return remoteAddr(remoteAddr::toString);
    }

    public HttpRequestMdcConfigProperties remoteAddr(Supplier<String> remoteAddr) {
        put("http.request.remote-addr", this::isRemoteAddr, remoteAddr);
        return this;
    }

    public HttpRequestMdcConfigProperties remoteHost(InetSocketAddress remoteHost) {
        if (remoteHost == null || !isRemoteHost()) return this;
        return remoteHost(remoteHost::toString);
    }

    public HttpRequestMdcConfigProperties remoteHost(Supplier<String> remoteHost) {
        put("http.request.remote-host", this::isRemoteHost, remoteHost);
        return this;
    }

    private void put(String key, BooleanSupplier enabled, Supplier<?> value) {
        if (enabled.getAsBoolean()) {
            put(key, value);
        }
    }

    private void put(String key, Supplier<?> value) {
        Object val = value.get();
        String svalue = val == null ? null : String.valueOf(val);
        put(key, svalue);
    }

    private void put(@NonNull String key, String value) {
        MDC.put(key, value);
    }

    /**
     * @return the id provided by the {@code traceId} header, {@code http.request.id} header, or a
     *     new monotonically increating UID if no such header is present
     */
    public static String findOrCreateRequestId(Supplier<HttpHeaders> headers) {
        return findRequestId(headers).orElseGet(() -> newRequestId());
    }

    /**
     * @return a new monotonically increating UID
     */
    public static String newRequestId() {
        return UlidCreator.getMonotonicUlid().toLowerCase();
    }

    /**
     * Obtains the request id, if present, fromt the {@code trace-id}, {@code http.request.id}, or
     * {@code x-request-id} request headers.
     */
    public static Optional<String> findRequestId(Supplier<HttpHeaders> headers) {
        HttpHeaders httpHeaders = headers.get();
        return header("trace-id", httpHeaders)
                .or(() -> header(REQUEST_ID_HEADER, httpHeaders))
                .or(() -> header("X-Request-ID", httpHeaders));
    }

    private static Optional<String> header(String name, HttpHeaders headers) {
        return Optional.ofNullable(headers.get(name)).filter(l -> !l.isEmpty()).map(l -> l.get(0));
    }
}
