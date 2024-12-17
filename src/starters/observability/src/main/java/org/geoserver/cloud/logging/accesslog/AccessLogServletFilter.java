/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.accesslog;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/** Similar to {@link CommonsRequestLoggingFilter} but uses slf4j */
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class AccessLogServletFilter extends OncePerRequestFilter {

    private final @NonNull AccessLogFilterConfig config;

    public AccessLogServletFilter(@NonNull AccessLogFilterConfig conf) {
        this.config = conf;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } finally {
            String uri = request.getRequestURI();
            String method = request.getMethod();
            int statusCode = response.getStatus();
            config.log(method, statusCode, uri);
        }
    }
}
