/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.mdc.servlet;

import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.cloud.logging.mdc.config.SpringEnvironmentMdcConfigProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class SpringEnvironmentMdcFilter extends OncePerRequestFilter {

    private final @NonNull Environment env;
    private final @NonNull Optional<BuildProperties> buildProperties;
    private final @NonNull SpringEnvironmentMdcConfigProperties config;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            config.addEnvironmentProperties(env, buildProperties);
        } finally {
            chain.doFilter(request, response);
        }
    }
}
