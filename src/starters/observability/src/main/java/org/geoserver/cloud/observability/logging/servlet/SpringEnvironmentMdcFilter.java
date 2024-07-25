/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.observability.logging.servlet;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.slf4j.MDC;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
public class SpringEnvironmentMdcFilter extends OncePerRequestFilter {

    private final @NonNull Environment env;
    private final @NonNull Optional<BuildProperties> buildProperties;
    private final @NonNull SpringEnvironmentMdcConfigProperties config;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            addEnvironmentProperties();
        } finally {
            chain.doFilter(request, response);
        }
    }

    private void addEnvironmentProperties() {
        if (config.isName())
            MDC.put("application.name", env.getProperty("spring.application.name"));

        putVersion();
        putInstanceId();

        if (config.isActiveProfiles())
            MDC.put(
                    "spring.profiles.active",
                    Stream.of(env.getActiveProfiles()).collect(Collectors.joining(",")));
    }

    private void putVersion() {
        if (config.isVersion()) {
            buildProperties
                    .map(BuildProperties::getVersion)
                    .ifPresent(v -> MDC.put("application.version", v));
        }
    }

    private void putInstanceId() {
        if (!config.isInstanceId() || null == config.getInstanceIdProperties()) return;

        for (String prop : config.getInstanceIdProperties()) {
            String value = env.getProperty(prop);
            if (StringUtils.hasText(value)) {
                MDC.put("application.instance.id", value);
                return;
            }
        }
    }
}
