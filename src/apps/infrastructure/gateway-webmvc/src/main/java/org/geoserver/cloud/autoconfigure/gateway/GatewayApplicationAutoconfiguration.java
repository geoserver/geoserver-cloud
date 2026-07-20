/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gateway;

import org.geoserver.cloud.gateway.filter.GeoServerGatewayFilterFunctions;
import org.geoserver.cloud.gateway.filter.ProxyExceptionFilter;
import org.geoserver.cloud.gateway.predicate.GeoServerGatewayRequestPredicates;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.server.mvc.filter.FilterSupplier;
import org.springframework.cloud.gateway.server.mvc.predicate.PredicateSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/** @since 3.0.0 */
@AutoConfiguration
@EnableConfigurationProperties({
    GlobalCorsProperties.class,
    SecureHeadersProperties.class,
    SharedAuthConfigurationProperties.class
})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class GatewayApplicationAutoconfiguration {

    /**
     * Normalizes the {@code basepath} property before gateway route definitions bind it: {@code /}, trailing slashes
     * and a missing leading slash all reduce to the canonical form (empty for the root path). Registered as a
     * {@link BeanFactoryPostProcessor} to run after every property source is in place (including remote config from
     * consul or the config server) and before route properties bind.
     *
     * @see NormalizedBasePathPropertySource
     */
    @Bean
    static BeanFactoryPostProcessor gatewayBasePathNormalizer(ConfigurableEnvironment environment) {
        return beanFactory -> NormalizedBasePathPropertySource.register(environment);
    }

    /**
     * CORS filter driven by {@link GlobalCorsProperties}. SCG Server MVC does not support the WebFlux
     * {@code globalcors} config, so we bind the same YAML structure and register a standard servlet {@link CorsFilter}.
     */
    @Bean
    CorsFilter gatewayCorsFilter(GlobalCorsProperties corsProperties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        corsProperties.getCorsConfigurations().forEach(source::registerCorsConfiguration);
        return new CorsFilter(source);
    }

    /**
     * Servlet filter that catches proxy connection errors (e.g. backend down) and returns 502 Bad Gateway with a
     * concise log message, instead of letting Tomcat log a full stack trace.
     */
    @Bean
    ProxyExceptionFilter proxyExceptionFilter() {
        return new ProxyExceptionFilter();
    }

    /**
     * Single filter supplier exposing all custom gateway filters ({@code StripBasePath}, {@code RouteProfile},
     * {@code SecureHeaders}, {@code SharedAuth}) for YAML route configuration.
     *
     * @see GeoServerGatewayFilterFunctions
     */
    @Bean
    FilterSupplier gatewayFiltersSupplier() {
        return new GeoServerGatewayFilterFunctions.GeoServerGatewayFilterSupplier();
    }

    /**
     * Custom gateway predicate factory to support matching by regular expressions on both name and value of query
     * parameters.
     *
     * <p>E.g.:
     *
     * <pre>{@code
     * - id: wms_ows
     *   uri: http://wms:8080
     *   predicates:
     *     # match service=wms case insensitively
     *     - RegExpQuery=(?i:service),(?i:wms)
     * }</pre>
     */
    @Bean
    PredicateSupplier gatewayRequestPredicatesSupplier() {
        return new GeoServerGatewayRequestPredicates.GeoServerGatewayPredicateSupplier();
    }
}
