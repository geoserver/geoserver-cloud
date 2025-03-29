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

/**
 * Filter that adds Spring Environment properties to the MDC (Mapped Diagnostic Context).
 * <p>
 * This filter enriches the MDC with application-specific information from the Spring Environment
 * and BuildProperties. The included properties are configured through {@link SpringEnvironmentMdcConfigProperties}
 * and can include:
 * <ul>
 *   <li>Application name</li>
 *   <li>Application version (from BuildProperties)</li>
 *   <li>Instance ID</li>
 *   <li>Active profiles</li>
 * </ul>
 * <p>
 * Adding these properties to the MDC makes them available to all logging statements, providing
 * valuable context for log analysis, especially in distributed microservice environments.
 * <p>
 * This filter extends {@link OncePerRequestFilter} to ensure it's only applied once per request,
 * even in a nested dispatch scenario (e.g., forward).
 *
 * @see SpringEnvironmentMdcConfigProperties
 * @see org.slf4j.MDC
 */
@RequiredArgsConstructor
public class SpringEnvironmentMdcFilter extends OncePerRequestFilter {

    private final @NonNull Environment env;
    private final @NonNull Optional<BuildProperties> buildProperties;
    private final @NonNull SpringEnvironmentMdcConfigProperties config;

    /**
     * Main filter method that adds Spring Environment properties to the MDC.
     * <p>
     * This method adds application-specific information from the Spring Environment
     * to the MDC before allowing the request to proceed through the filter chain.
     * The properties are added in a try-finally block to ensure they're available
     * throughout the request processing, even if an exception occurs.
     *
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @param chain the filter chain to execute
     * @throws ServletException if a servlet exception occurs
     * @throws IOException if an I/O exception occurs
     */
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
