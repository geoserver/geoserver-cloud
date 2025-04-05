/*
 * (c) 2024-2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.app;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Auto-configuration that enables Reactor Context Propagation.
 * <p>
 * This is important for ensuring MDC values are properly propagated in reactive code,
 * particularly when using WebFlux or Spring Cloud Gateway.
 * <p>
 * The configuration sets the system property "reactor.context.propagation.enabled" to "true"
 * which enables automatic context propagation in Reactor 3.5.0+.
 */
@AutoConfiguration
@ConditionalOnClass(name = "reactor.core.publisher.Mono")
@ConditionalOnProperty(
        name = "geoserver.cloud.reactor.context-propagation.enabled",
        havingValue = "true",
        matchIfMissing = true)
@Slf4j
public class ReactorContextPropagationAutoConfiguration {

    @PostConstruct
    void enableReactorContextPropagation() {
        if (System.getProperty("reactor.context.propagation.enabled") == null) {
            log.info("Enabling Reactor Context Propagation");
            System.setProperty("reactor.context.propagation.enabled", "true");
        }
    }
}
