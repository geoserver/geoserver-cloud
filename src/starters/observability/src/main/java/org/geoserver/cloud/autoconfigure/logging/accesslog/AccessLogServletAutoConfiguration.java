/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.logging.accesslog;

import org.geoserver.cloud.logging.accesslog.AccessLogFilterConfig;
import org.geoserver.cloud.logging.accesslog.AccessLogServletFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for access logging in Servlet applications.
 * <p>
 * This configuration automatically sets up the {@link AccessLogServletFilter} for servlet web applications,
 * enabling HTTP request access logging. The filter captures key information about each request
 * and logs it at the appropriate level based on the configuration.
 * <p>
 * The configuration activates only when the following conditions are met:
 * <ul>
 *   <li>The application is a Servlet web application ({@code spring.main.web-application-type=servlet})</li>
 *   <li>The property {@code logging.accesslog.enabled} is set to {@code true}</li>
 * </ul>
 * <p>
 * Access log properties are controlled through the {@link AccessLogFilterConfig} class,
 * which allows defining patterns for requests to be logged at different levels (info, debug, trace).
 * <p>
 * This auto-configuration is compatible with and complements the WebFlux-based
 * {@link AccessLogWebFluxAutoConfiguration}. Both can be present in an application that
 * supports either servlet or reactive web models, but only one will be active based on the
 * web application type.
 *
 * @see AccessLogServletFilter
 * @see AccessLogFilterConfig
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnProperty(name = AccessLogFilterConfig.ENABLED_KEY, havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(AccessLogFilterConfig.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public class AccessLogServletAutoConfiguration {

    /**
     * Creates the AccessLogServletFilter bean for Servlet applications.
     * <p>
     * This bean is responsible for logging HTTP requests based on the provided configuration.
     * The filter captures key information about each request and logs it at the appropriate level
     * based on the URL patterns defined in the configuration.
     * <p>
     * The filter is configured with the {@link AccessLogFilterConfig} which determines:
     * <ul>
     *   <li>Which URL patterns are logged</li>
     *   <li>What log level (info, debug, trace) is used for each pattern</li>
     * </ul>
     *
     * @param conf the access log filter configuration properties
     * @return the configured AccessLogServletFilter bean
     */
    @Bean
    AccessLogServletFilter accessLogFilter(AccessLogFilterConfig conf) {
        return new AccessLogServletFilter(conf);
    }
}
