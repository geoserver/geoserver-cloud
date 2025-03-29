/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.accesslog;

import java.net.URI;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.web.reactive.filter.OrderedWebFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * A WebFlux filter for logging HTTP request access in a reactive environment.
 * <p>
 * This filter logs HTTP requests based on the provided {@link AccessLogFilterConfig} configuration.
 * It captures the following information about each request:
 * <ul>
 *   <li>HTTP method (GET, POST, etc.)</li>
 *   <li>URI path</li>
 *   <li>Status code</li>
 *   <li>Processing duration</li>
 * </ul>
 * <p>
 * Note: This filter does not support MDC propagation in Spring Boot 2.7 WebFlux applications.
 * For full MDC support, use the Spring Boot 3 compatible module.
 * <p>
 * This filter is configured with {@link Ordered#LOWEST_PRECEDENCE} to ensure it executes
 * after all other filters, capturing the complete request processing time and final status code.
 */
@Slf4j
public class AccessLogWebfluxFilter implements OrderedWebFilter {

    private final @NonNull AccessLogFilterConfig config;

    /**
     * Constructs an AccessLogWebfluxFilter with the given configuration.
     *
     * @param config the configuration for access logging
     */
    public AccessLogWebfluxFilter(@NonNull AccessLogFilterConfig config) {
        this.config = config;
    }

    /**
     * Returns the order of this filter in the filter chain.
     * <p>
     * This filter is set to {@link Ordered#LOWEST_PRECEDENCE} to ensure it executes after
     * all other filters in the chain. This allows it to capture the complete request
     * processing time and the final status code.
     *
     * @return the lowest precedence order value
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * Main filter method that processes WebFlux requests and logs access information.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Checks if the request URI should be logged based on the configuration</li>
     *   <li>Captures the request start time, method, and URI</li>
     *   <li>Continues the filter chain</li>
     *   <li>After the response is complete, retrieves the status code and calculates duration</li>
     *   <li>Logs the request</li>
     * </ol>
     * <p>
     * If the request URI doesn't match any of the configured patterns, the request is not logged
     * and the filter simply passes control to the next filter in the chain.
     *
     * @param exchange the current server exchange
     * @param chain the filter chain to delegate to
     * @return a Mono completing when the request handling is done
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        URI uri = exchange.getRequest().getURI();
        if (!config.shouldLog(uri)) {
            return chain.filter(exchange);
        }

        // Capture request start time
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethodValue();
        String uriPath = uri.toString();

        // Store initial MDC state
        Map<String, String> initialMdc = MDC.getCopyOfContextMap();

        return chain.filter(exchange).doFinally(signalType -> {
            try {
                // Calculate request duration
                long duration = System.currentTimeMillis() - startTime;

                // Get status code if available, or use 0 if not set
                Integer statusCode = exchange.getResponse().getRawStatusCode();
                if (statusCode == null) statusCode = 0;

                // Log the request without MDC context
                config.log(method, statusCode, uriPath);

                if (log.isTraceEnabled()) {
                    log.trace("Request {} {} {} completed in {}ms", method, statusCode, uriPath, duration);
                }
            } finally {
                // Restore initial MDC state if any
                if (initialMdc != null) MDC.setContextMap(initialMdc);
                else MDC.clear();
            }
        });
    }
}
