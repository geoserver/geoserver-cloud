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
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

/** Contributes HTTP request properties to MDC attributes */
@Data
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

    public HttpRequestMdcConfigProperties headers(Supplier<HttpHeaders> headers) {
        if (isHeaders()) {
            HttpHeaders httpHeaders = headers.get();
            httpHeaders.forEach(this::putHeader);
        }
        return this;
    }

    public HttpRequestMdcConfigProperties cookies(Supplier<MultiValueMap<String, HttpCookie>> cookies) {
        if (isCookies()) {
            cookies.get().values().forEach(this::putCookie);
        }
        return this;
    }

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

    private boolean includeHeader(String headerName) {
        if ("cookie".equalsIgnoreCase(headerName)) return false;
        return getHeadersPattern().matcher(headerName).matches();
    }

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
        return remoteAddr(remoteAddr::toString);
    }

    public HttpRequestMdcConfigProperties remoteAddr(Supplier<String> remoteAddr) {
        put("http.request.remote-addr", this::isRemoteAddr, remoteAddr);
        return this;
    }

    public HttpRequestMdcConfigProperties remoteHost(InetSocketAddress remoteHost) {
        return remoteAddr(remoteHost::toString);
    }

    public HttpRequestMdcConfigProperties remoteHost(Supplier<String> remoteHost) {
        put("http.request.remote-host", this::isRemoteAddr, remoteHost);
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
