/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.logging.mdc.webflux;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

/**
 * Utility class to access Reactor Context from non-reactive code.
 * <p>
 * In reactive applications using WebFlux, the standard SLF4J MDC (Mapped Diagnostic Context) cannot be used
 * directly because the reactive pipeline may not execute in the same thread that initiated the request.
 * This class provides a bridge between the Reactor Context and MDC.
 * <p>
 * It allows extracting MDC information from the Reactor Context chain, making it accessible to logging
 * statements in both reactive and non-reactive code. This is especially useful for access logging and
 * other cross-cutting logging concerns.
 * <p>
 * The MDC data is stored in the Reactor Context under the key {@link #MDC_CONTEXT_KEY}.
 */
@UtilityClass
public class ReactorContextHolder {

    /**
     * Key used to store MDC data in Reactor context.
     * <p>
     * This constant defines the key under which the MDC map is stored in the Reactor Context.
     * It should be used consistently across all code that needs to access or modify the MDC data
     * in a reactive context.
     */
    public static final String MDC_CONTEXT_KEY = "MDC_CONTEXT";

    /**
     * Retrieves the MDC map from the current thread's context.
     * <p>
     * This method tries to get MDC information from the thread-local MDC context,
     * which is typically set by MDCWebFilter in WebFlux applications. If no MDC
     * context is found, it returns an empty map.
     * <p>
     * This approach avoids blocking operations in reactive code while still providing
     * access to MDC data for logging purposes.
     *
     * @return the MDC map from context or an empty map if none exists
     */
    public static Map<String, String> getMdcMap() {
        // Check thread-local MDC context, which might have been set by MDCWebFilter
        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        if (mdcMap != null && !mdcMap.isEmpty()) {
            return mdcMap;
        }

        // If we're in a reactive context, we should have populated the MDC via doOnSubscribe
        // in the MDCWebFilter, but as a fallback, return an empty map
        return new HashMap<>();
    }

    /**
     * Sets MDC values from a map into the current thread's MDC context.
     * <p>
     * This method provides a convenient way to transfer MDC values from a Reactor Context
     * into the current thread's MDC context before logging operations. It's particularly
     * useful in operators like doOnNext, doOnEach, or doOnSubscribe.
     *
     * @param mdcValues the MDC values to set
     */
    public static void setThreadLocalMdc(Map<String, String> mdcValues) {
        if (mdcValues != null && !mdcValues.isEmpty()) {
            // Save current MDC
            Map<String, String> oldMdc = MDC.getCopyOfContextMap();

            try {
                // Set MDC values for current thread
                MDC.setContextMap(mdcValues);
            } catch (Exception ex) {
                // Restore previous MDC if there was a problem
                if (oldMdc != null) {
                    MDC.setContextMap(oldMdc);
                } else {
                    MDC.clear();
                }
            }
        }
    }

    /**
     * Helper method to ensure MDC values are available when logging.
     * <p>
     * This method can be used in hooks like doOnEach or doOnNext to ensure
     * that MDC values from the reactor context are set in the current thread
     * for logging purposes.
     *
     * @param context The reactor context view to extract MDC values from
     */
    public static void setMdcFromContext(reactor.util.context.ContextView context) {
        try {
            if (context.hasKey(MDC_CONTEXT_KEY)) {
                Object mdcObj = context.get(MDC_CONTEXT_KEY);
                if (mdcObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> contextMdc = (Map<String, String>) mdcObj;
                    MDC.setContextMap(contextMdc);
                }
            }
        } catch (Exception e) {
            // Just log and continue if there's an issue with MDC
            System.err.println("Error setting MDC from context: " + e.getMessage());
        }
    }

    /**
     * Gets the MDC map from the reactor context in the given Mono chain.
     * <p>
     * This method allows you to explicitly retrieve the MDC map from within a reactive chain
     * without using blocking operations.
     *
     * @param mono the Mono to retrieve context from
     * @return a new Mono that will emit the MDC map
     */
    @SuppressWarnings("unchecked")
    public static Mono<Map<String, String>> getMdcMapFromContext(Mono<?> mono) {
        return mono.flatMap(ignored -> Mono.deferContextual(ctx -> {
            if (ctx.hasKey(MDC_CONTEXT_KEY)) {
                Object mdcObj = ctx.get(MDC_CONTEXT_KEY);
                if (mdcObj instanceof Map) {
                    return Mono.just((Map<String, String>) mdcObj);
                }
            }
            return Mono.just(new HashMap<String, String>());
        }));
    }
}
