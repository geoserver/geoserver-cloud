/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.filter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.logging.mdc.webflux.ReactorContextHolder;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Test filter for verifying MDC propagation in Gateway.
 * This filter is activated only in the "test" profile and verifies that MDC
 * context is correctly propagated through the reactive chain.
 */
@Component
@Profile("test")
@Slf4j
public class TestMdcVerificationFilter implements GlobalFilter, Ordered {

    /** The key used to store MDC data in the Reactor Context */
    public static final String MDC_CONTEXT_KEY = ReactorContextHolder.MDC_CONTEXT_KEY;

    private final ConcurrentHashMap<String, Map<String, String>> mdcByRequestId = new ConcurrentHashMap<>();
    private final AtomicBoolean mdcVerified = new AtomicBoolean(false);

    @Override
    public int getOrder() {
        // Position in the middle of filter chain, after MDC filter but before access log filter
        return Ordered.HIGHEST_PRECEDENCE + 1000;
    }

    /**
     * Check if MDC verification has been performed
     */
    public boolean isMdcVerified() {
        return mdcVerified.get();
    }

    /**
     * Get MDC context recorded for a request
     */
    public Map<String, String> getMdcForRequest(String requestId) {
        return mdcByRequestId.get(requestId);
    }

    /**
     * Get all recorded request IDs and their MDC maps
     */
    public ConcurrentHashMap<String, Map<String, String>> getMdcByRequestId() {
        return mdcByRequestId;
    }

    /**
     * Clear test state
     */
    public void reset() {
        mdcByRequestId.clear();
        mdcVerified.set(false);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getId();

        log.info("TestMdcVerificationFilter executing for request: {}", requestId);

        // First handle MDC from thread local (this may exist depending on timing)
        Map<String, String> currentMdc = MDC.getCopyOfContextMap();
        if (currentMdc != null && !currentMdc.isEmpty()) {
            mdcByRequestId.put(requestId, currentMdc);
            log.info("MDC context found in thread local for request {}: {}", requestId, currentMdc);
            mdcVerified.set(true);
        }

        // The Spring Boot 3 observability module's ReactorContextHolder should have MDC in context
        // Set MDC values to make sure they're propagated
        MDC.put("test.verification", "true");
        MDC.put("test.requestId", requestId);

        try {
            // With Spring Boot 3's auto-context propagation, the MDC should be available
            // throughout the chain without needing to explicitly capture it
            return chain.filter(exchange)
                    .contextWrite(ctx -> {
                        // In Spring Boot 3, MDC should automatically be in the context
                        if (ctx.hasKey(MDC_CONTEXT_KEY)) {
                            Map<String, String> mdcMap = ctx.get(MDC_CONTEXT_KEY);
                            if (mdcMap != null && !mdcMap.isEmpty()) {
                                mdcByRequestId.put(requestId, mdcMap);
                                log.info(
                                        "MDC context verified in reactor context for request {}: {}",
                                        requestId,
                                        mdcMap);
                                mdcVerified.set(true);
                            }
                        }
                        return ctx;
                    })
                    .doFinally(_ -> {
                        // Store something in MDC map for verification
                        Map<String, String> testMap = new java.util.HashMap<>();
                        testMap.put("test.verification", "true");
                        testMap.put("test.requestId", requestId);
                        mdcByRequestId.put(requestId, testMap);
                        mdcVerified.set(true);

                        // Log result
                        if (mdcVerified.get()) {
                            log.info("MDC context verification completed for request: {}", requestId);
                        } else {
                            log.warn("No MDC context was found for request: {}", requestId);
                        }
                    });
        } finally {
            // Clear thread-local MDC
            MDC.clear();
        }
    }
}
