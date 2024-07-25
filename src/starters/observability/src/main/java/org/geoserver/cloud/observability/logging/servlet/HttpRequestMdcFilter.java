/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.observability.logging.servlet;

import com.github.f4b6a3.ulid.UlidCreator;
import com.google.common.collect.Streams;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RequiredArgsConstructor
public class HttpRequestMdcFilter extends OncePerRequestFilter {

    private final @NonNull HttpRequestMdcConfigProperties config;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            if (request instanceof HttpServletRequest req) addRequestMdcProperties(req);
        } finally {
            chain.doFilter(request, response);
        }
    }

    private void addRequestMdcProperties(HttpServletRequest req) {
        HttpSession session = req.getSession(false);

        put("http.request.id", config::isId, () -> requestId(req));
        put("http.request.remote-addr", config::isRemoteAddr, req::getRemoteAddr);
        put("http.request.remote-host", config::isRemoteHost, req::getRemoteHost);

        put("http.request.method", config::isMethod, req::getMethod);
        put("http.request.url", config::isUrl, req::getRequestURL);
        putRequestParams(req);
        put("http.request.query-string", config::isQueryString, req::getQueryString);
        put(
                "http.request.session.id",
                config::isSessionId,
                () -> session == null ? null : session.getId());
        put(
                "http.request.session.started",
                config::isSessionId,
                () -> session == null ? null : !session.isNew());
        addHeaders(req);
        addCookies(req);
    }

    private void putRequestParams(HttpServletRequest req) {
        if (config.isParameters()) {
            Streams.stream(req.getParameterNames().asIterator())
                    .forEach(
                            name ->
                                    put(
                                            "http.request.parameter.%s".formatted(name),
                                            requestParam(name, req)));
        }
    }

    private String requestParam(String name, HttpServletRequest req) {
        String[] values = req.getParameterValues(name);
        if (null == values) return null;
        if (values.length == 1) return values[0];
        return null;
    }

    private void addHeaders(HttpServletRequest req) {
        if (config.isHeaders()) {
            Streams.stream(req.getHeaderNames().asIterator())
                    .filter(h -> !"cookie".equalsIgnoreCase(h))
                    .filter(this::includeHeader)
                    .forEach(name -> putHeader(name, req));
        }
    }

    private void putHeader(String name, HttpServletRequest req) {
        put("http.request.header.%s".formatted(name), () -> getHeader(name, req));
    }

    private String getHeader(String name, HttpServletRequest req) {
        return Streams.stream(req.getHeaders(name).asIterator()).collect(Collectors.joining(","));
    }

    private boolean includeHeader(String headerName) {
        return config.getHeadersPattern().matcher(headerName).matches();
    }

    private void addCookies(HttpServletRequest req) {
        if (config.isCookies()) {
            Cookie[] cookies = req.getCookies();
            if (null != cookies) {
                Stream.of(cookies).forEach(this::put);
            }
        }
    }

    private void put(Cookie c) {
        String key = "http.request.cookie.%s".formatted(c.getName());
        String value = MDC.get(key);
        if (value == null) {
            value = c.getValue();
        } else {
            value = "%s;%s".formatted(value, c.getValue());
        }
        MDC.put(key, value);
    }

    private void put(String key, BooleanSupplier enabled, Supplier<Object> value) {
        if (enabled.getAsBoolean()) {
            put(key, value);
        }
    }

    private void put(String key, Supplier<Object> value) {
        Object val = value.get();
        String svalue = val == null ? null : String.valueOf(val);
        put(key, svalue);
    }

    private void put(@NonNull String key, String value) {
        MDC.put(key, value);
    }

    /**
     * @return the id provided by the {@code http.request.id} header, or a new monotonically
     *     increating UID if no such header is present
     */
    private String requestId(HttpServletRequest req) {
        return Optional.ofNullable(req.getHeader("http.request.id"))
                .orElseGet(() -> UlidCreator.getMonotonicUlid().toLowerCase());
    }
}
