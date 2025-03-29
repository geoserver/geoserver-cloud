/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.accesslog;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.logging.mdc.webflux.ReactorContextHolder;
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
 * The filter leverages MDC (Mapped Diagnostic Context) for enriched logging, retrieving
 * the MDC map from the Reactor Context using {@link ReactorContextHolder}. This allows
 * the access logs to include all the MDC attributes set by the {@link org.geoserver.cloud.logging.mdc.webflux.MDCWebFilter}.
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
     *   <li>Saves the initial MDC state</li>
     *   <li>Continues the filter chain</li>
     *   <li>After the response is complete, retrieves the status code and calculates duration</li>
     *   <li>Retrieves MDC from the Reactor Context</li>
     *   <li>Logs the request with appropriate MDC context</li>
     *   <li>Restores the original MDC state</li>
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
        String method = request.getMethod().name();
        String uriPath = uri.toString();

        // Store initial MDC state
        Map<String, String> initialMdc = MDC.getCopyOfContextMap();

        // Use doOnEach to ensure we have appropriate MDC context during the logging phase
        return chain.filter(exchange).doFinally(signalType -> {
            // Get MDC from Reactor context or thread-local before logging
            Map<String, String> mdcMap = null;
            try {
                Object mdcObj = Mono.deferContextual(ctx -> {
                            if (ctx.hasKey(ReactorContextHolder.MDC_CONTEXT_KEY)) {
                                return Mono.just(ctx.get(ReactorContextHolder.MDC_CONTEXT_KEY));
                            }
                            return Mono.empty();
                        })
                        .defaultIfEmpty(new HashMap<>())
                        .block();

                if (mdcObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> contextMdc = (Map<String, String>) mdcObj;
                    mdcMap = contextMdc;
                }
            } catch (Exception e) {
                // Fallback to thread-local MDC if there's an error accessing context
                mdcMap = MDC.getCopyOfContextMap();
            }

            if (mdcMap == null) {
                mdcMap = new HashMap<>();
            }

            // Log with the MDC context
            logRequestCompletion(exchange, startTime, method, uriPath, initialMdc, mdcMap);
        });
    }

    /**
     * Logs the completion of an HTTP request with appropriate MDC context.
     * <p>
     * This method handles the logging of access information after the request is completed,
     * including:
     * <ul>
     *   <li>Calculating the request duration</li>
     *   <li>Retrieving the final status code</li>
     *   <li>Managing MDC context for structured logging</li>
     *   <li>Ensuring proper MDC cleanup</li>
     * </ul>
     *
     * @param exchange the server exchange containing the response
     * @param startTime the time when the request processing started
     * @param method the HTTP method of the request
     * @param uriPath the URI path of the request
     * @param initialMdc the initial MDC state to restore after logging
     * @param contextMdc the MDC context from the reactor context
     */
    private void logRequestCompletion(
            ServerWebExchange exchange,
            long startTime,
            String method,
            String uriPath,
            Map<String, String> initialMdc,
            Map<String, String> contextMdc) {

        try {
            // Calculate request duration
            long duration = System.currentTimeMillis() - startTime;

            // Get status code if available, or use 0 if not set
            Integer statusCode = exchange.getResponse().getRawStatusCode();
            if (statusCode == null) statusCode = 0;

            logWithAppropriateContext(method, statusCode, uriPath, duration, contextMdc);
        } finally {
            // Restore initial MDC state
            if (initialMdc != null) MDC.setContextMap(initialMdc);
            else MDC.clear();
        }
    }

    /**
     * Logs the request with the appropriate MDC context if available.
     *
     * @param method the HTTP method
     * @param statusCode the response status code
     * @param uriPath the request URI path
     * @param duration the request processing duration in milliseconds
     * @param contextMdc the MDC context from the reactor context, may be null or empty
     */
    private void logWithAppropriateContext(
            String method, Integer statusCode, String uriPath, long duration, Map<String, String> contextMdc) {

        if (contextMdc != null && !contextMdc.isEmpty()) {
            logWithMdcContext(method, statusCode, uriPath, duration, contextMdc);
        } else {
            logWithoutMdcContext(method, statusCode, uriPath, duration);
        }
    }

    /**
     * Logs the request with MDC context from the reactor context.
     */
    private void logWithMdcContext(
            String method, Integer statusCode, String uriPath, long duration, Map<String, String> contextMdc) {

        // Save original MDC
        Map<String, String> oldMdc = MDC.getCopyOfContextMap();

        try {
            // Set MDC from reactor context for logging
            MDC.setContextMap(contextMdc);

            // Log the request with MDC context
            config.log(method, statusCode, uriPath);

            if (log.isTraceEnabled()) {
                log.trace("Request {} {} {} completed in {}ms", method, statusCode, uriPath, duration);
            }
        } finally {
            // Restore original MDC
            if (oldMdc != null) MDC.setContextMap(oldMdc);
            else MDC.clear();
        }
    }

    /**
     * Logs the request without MDC context when none is available.
     */
    private void logWithoutMdcContext(String method, Integer statusCode, String uriPath, long duration) {
        // Log without MDC context if not available
        config.log(method, statusCode, uriPath);

        if (log.isTraceEnabled()) {
            log.trace("Request {} {} {} completed in {}ms (no MDC context)", method, statusCode, uriPath, duration);
        }
    }
}
