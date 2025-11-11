/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.logging.gateway;

import lombok.RequiredArgsConstructor;
import org.geoserver.cloud.autoconfigure.logging.accesslog.webflux.AccessLogWebFluxAutoConfiguration;
import org.geoserver.cloud.autoconfigure.logging.mdc.webflux.LoggingMDCWebFluxAutoConfiguration;
import org.geoserver.cloud.logging.accesslog.AccessLogFilterConfig;
import org.geoserver.cloud.logging.accesslog.AccessLogWebfluxFilter;
import org.geoserver.cloud.logging.mdc.webflux.MDCWebFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Auto-configuration for integrating WebFlux MDC filters with Spring Cloud Gateway.
 * <p>
 * This configuration ensures that both MDC propagation and access logging
 * are properly configured and registered as global filters in the gateway.
 * <p>
 * <h2>Why Both WebFilter and GlobalFilter?</h2>
 * While WebFilters like MDCWebFilter and AccessLogWebfluxFilter will automatically run in Spring WebFlux
 * applications, Spring Cloud Gateway uses its own filter chain mechanism with important differences:
 * <ul>
 *   <li><b>Separate Filter Chains:</b> Spring WebFlux uses {@code WebFilter} while Spring Cloud Gateway
 *       uses {@code GlobalFilter}. These are two distinct filter chains that execute independently.</li>
 *   <li><b>Execution Order:</b> The GlobalFilter chain executes before the WebFilter chain in Gateway,
 *       so Gateway-specific components (routes, predicates, custom filters) would miss MDC context
 *       without proper integration.</li>
 *   <li><b>Complete Request Tracing:</b> To ensure consistent logging across the entire request
 *       lifecycle, MDC context must be available from the earliest point of request processing.</li>
 * </ul>
 * <p>
 * Without these adapters, you might see logs from the WebFlux filter chain with MDC context, but logs
 * from Gateway components would lack this context, resulting in incomplete or inconsistent logging.
 * <p>
 * This configuration is conditional on the presence of MDCWebFilter and Spring Cloud Gateway classes,
 * which are typically provided by the {@code gs-cloud-starter-observability} module's
 * optional dependency on Spring Cloud Gateway.
 * <p>
 * It creates adapter filters that bridge between Spring WebFlux's WebFilter interface and
 * Spring Cloud Gateway's GlobalFilter interface, ensuring complete MDC context propagation.
 */
@AutoConfiguration(after = {AccessLogWebFluxAutoConfiguration.class, LoggingMDCWebFluxAutoConfiguration.class})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnClass(name = {"org.springframework.cloud.gateway.filter.GlobalFilter"})
@ConditionalOnBean(MDCWebFilter.class)
public class GatewayMdcAutoConfiguration {

    /**
     * Creates a GlobalFilter adapter for the MDCWebFilter.
     * <p>
     * This adapter allows the Spring WebFlux MDCWebFilter to be used as a
     * Spring Cloud Gateway GlobalFilter, ensuring MDC context is properly
     * propagated throughout the gateway filter chain.
     *
     * @param mdcWebFilter the MDCWebFilter bean to adapt
     * @return a GlobalFilter that delegates to the MDCWebFilter
     */
    @Bean
    GlobalFilter mdcGlobalFilter(MDCWebFilter mdcWebFilter) {
        return new MdcGlobalFilterAdapter(mdcWebFilter);
    }

    /**
     * Creates a Gateway-specific AccessLogFilterConfig.
     * <p>
     * This configuration is used by the Gateway's access log filter
     * and is separate from any config used by WebFlux filters.
     * <p>
     * This bean is primary to ensure it takes precedence over any other
     * AccessLogFilterConfig beans that might exist in the context.
     *
     * @return a configuration for Gateway access logging
     */
    @Bean
    @org.springframework.context.annotation.Primary
    AccessLogFilterConfig gatewayAccessLogConfig() {
        return new AccessLogFilterConfig();
    }

    /**
     * Creates a GlobalFilter for access logging in the Gateway.
     * <p>
     * This filter logs HTTP requests processed by Spring Cloud Gateway.
     * It uses its own instance of AccessLogWebfluxFilter internally.
     * <p>
     * This is the primary access log filter for Gateway applications.
     * The standard WebFlux filter in AccessLogWebFluxAutoConfiguration is
     * automatically disabled in Gateway applications to avoid double-logging.
     *
     * @param gatewayAccessLogConfig the access log configuration
     * @return a GlobalFilter for access logging
     */
    @Bean
    GlobalFilter accessLogGlobalFilter(AccessLogFilterConfig gatewayAccessLogConfig) {
        // Create a dedicated AccessLogWebfluxFilter for the Gateway's GlobalFilter chain
        // By default, the WebFlux filter is not created in Gateway applications
        // (see ConditionalOnMissingClass in AccessLogWebFluxAutoConfiguration)
        AccessLogWebfluxFilter gatewayAccessLogFilter = new AccessLogWebfluxFilter(gatewayAccessLogConfig);
        return new AccessLogGlobalFilterAdapter(gatewayAccessLogFilter);
    }

    /**
     * Adapter to use MDCWebFilter as a GlobalFilter
     */
    @RequiredArgsConstructor
    static class MdcGlobalFilterAdapter implements GlobalFilter, Ordered {

        private final MDCWebFilter mdcWebFilter;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            // Delegate to our WebFilter implementation
            return mdcWebFilter.filter(exchange, chain::filter);
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }

    /**
     * Adapter to use AccessLogWebfluxFilter as a GlobalFilter
     * <p>
     * This adapter allows AccessLogWebfluxFilter to be used in the GlobalFilter chain.
     * This adapter is the only access log filter in Gateway applications because the
     * standard WebFlux filter is disabled by @ConditionalOnMissingClass in
     * AccessLogWebFluxAutoConfiguration to prevent double-logging.
     */
    @RequiredArgsConstructor
    static class AccessLogGlobalFilterAdapter implements GlobalFilter, Ordered {

        private final AccessLogWebfluxFilter accessLogFilter;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            // Delegate to our WebFilter implementation
            return accessLogFilter.filter(exchange, chain::filter);
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }
    }
}
