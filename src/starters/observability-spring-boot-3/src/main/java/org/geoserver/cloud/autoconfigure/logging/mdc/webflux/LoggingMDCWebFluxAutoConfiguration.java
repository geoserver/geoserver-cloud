/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.logging.mdc.webflux;

import java.util.Optional;
import org.geoserver.cloud.logging.mdc.config.AuthenticationMdcConfigProperties;
import org.geoserver.cloud.logging.mdc.config.HttpRequestMdcConfigProperties;
import org.geoserver.cloud.logging.mdc.config.SpringEnvironmentMdcConfigProperties;
import org.geoserver.cloud.logging.mdc.webflux.MDCWebFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Auto-configuration for MDC logging in WebFlux applications.
 * <p>
 * This configuration automatically sets up the {@link MDCWebFilter} for reactive web applications,
 * enabling MDC (Mapped Diagnostic Context) support in a WebFlux environment. The MDC context
 * is propagated through the reactive chain using Reactor Context, making diagnostic information
 * available for logging throughout the request processing.
 * <p>
 * The configuration activates only for reactive web applications (WebFlux) and provides the following:
 * <ul>
 *   <li>Configuration properties for controlling what information is included in the MDC</li>
 *   <li>The MDCWebFilter that populates and propagates the MDC</li>
 * </ul>
 * <p>
 * MDC properties are controlled through the following configuration properties classes:
 * <ul>
 *   <li>{@link AuthenticationMdcConfigProperties} - For user authentication information</li>
 *   <li>{@link HttpRequestMdcConfigProperties} - For HTTP request details</li>
 *   <li>{@link SpringEnvironmentMdcConfigProperties} - For application environment properties</li>
 * </ul>
 *
 * @see MDCWebFilter
 * @see AuthenticationMdcConfigProperties
 * @see HttpRequestMdcConfigProperties
 * @see SpringEnvironmentMdcConfigProperties
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnWebApplication(type = Type.REACTIVE)
@EnableConfigurationProperties({
    AuthenticationMdcConfigProperties.class,
    HttpRequestMdcConfigProperties.class,
    SpringEnvironmentMdcConfigProperties.class
})
public class LoggingMDCWebFluxAutoConfiguration {

    /**
     * Creates the MDCWebFilter bean for WebFlux applications.
     * <p>
     * This bean is responsible for populating the MDC with information from various sources
     * and propagating it through the reactive chain. The filter is configured with the
     * following dependencies:
     * <ul>
     *   <li>Authentication configuration - Controls user-related MDC attributes</li>
     *   <li>HTTP request configuration - Controls request-related MDC attributes</li>
     *   <li>Environment configuration - Controls application environment MDC attributes</li>
     *   <li>Spring Environment - For accessing application properties</li>
     *   <li>BuildProperties - For accessing application version information (optional)</li>
     * </ul>
     *
     * @param authConfig authentication MDC configuration properties
     * @param httpConfig HTTP request MDC configuration properties
     * @param envConfig application environment MDC configuration properties
     * @param env Spring environment for accessing application properties
     * @param buildProps build properties for accessing version information (optional)
     * @return the configured MDCWebFilter bean
     */
    @Bean
    MDCWebFilter mdcWebFilter(
            AuthenticationMdcConfigProperties authConfig,
            HttpRequestMdcConfigProperties httpConfig,
            SpringEnvironmentMdcConfigProperties envConfig,
            Environment env,
            Optional<BuildProperties> buildProps) {
        return new MDCWebFilter(authConfig, httpConfig, envConfig, env, buildProps);
    }
}
