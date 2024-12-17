/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.mdc.servlet;

import com.google.common.base.Suppliers;
import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.cloud.logging.mdc.config.HttpRequestMdcConfigProperties;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class HttpRequestMdcFilter extends OncePerRequestFilter {

    private final @NonNull HttpRequestMdcConfigProperties config;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            if (request instanceof HttpServletRequest req) addRequestMdcProperties(req);
        } finally {
            chain.doFilter(request, response);
        }
    }

    private void addRequestMdcProperties(HttpServletRequest req) {
        Supplier<HttpHeaders> headers = headers(req);
        config.id(headers)
                .remoteAddr(req::getRemoteAddr)
                .remoteHost(req::getRemoteHost)
                .method(req::getMethod)
                .url(req::getRequestURI)
                .queryString(req::getQueryString)
                .parameters(parameters(req))
                .sessionId(sessionId(req))
                .headers(headers)
                .cookies(cookies(req));
    }

    Supplier<MultiValueMap<String, String>> parameters(HttpServletRequest req) {
        return () -> {
            var map = new LinkedMultiValueMap<String, String>();
            Map<String, String[]> params = req.getParameterMap();
            params.forEach((k, v) -> map.put(k, v == null ? null : Arrays.asList(v)));
            return map;
        };
    }

    private Supplier<MultiValueMap<String, HttpCookie>> cookies(HttpServletRequest req) {
        return () -> {
            Cookie[] cookies = req.getCookies();
            var map = new LinkedMultiValueMap<String, HttpCookie>();
            if (null != cookies && cookies.length > 0) {
                for (Cookie c : cookies) {
                    map.add(c.getName(), new HttpCookie(c.getName(), c.getValue()));
                }
            }
            return map;
        };
    }

    private Supplier<String> sessionId(HttpServletRequest req) {
        return () -> Optional.ofNullable(req.getSession(false))
                .map(HttpSession::getId)
                .orElse(null);
    }

    private Supplier<HttpHeaders> headers(HttpServletRequest req) {
        return Suppliers.memoize(buildHeaders(req));
    }

    private com.google.common.base.Supplier<HttpHeaders> buildHeaders(HttpServletRequest req) {
        return () -> {
            HttpHeaders headers = new HttpHeaders();
            Streams.stream(req.getHeaderNames().asIterator())
                    .forEach(name -> headers.put(name, headerValue(name, req)));
            return headers;
        };
    }

    private List<String> headerValue(String name, HttpServletRequest req) {
        Enumeration<String> values = req.getHeaders(name);
        if (null == values) return List.of();
        return Streams.stream(values.asIterator()).toList();
    }
}
