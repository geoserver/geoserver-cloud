/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Application context initializer that sets the {@code cpu.cores} system property.
 *
 * <p>This initializer runs early in the Spring Boot startup lifecycle, before configuration
 * properties are bound. It sets the {@code cpu.cores} system property to the value returned by
 * {@link Runtime#availableProcessors()}, which reflects the number of CPU cores available to the
 * JVM (respecting container CPU limits).
 *
 * <p>This property enables dynamic configuration of Control-Flow limits based on available CPU
 * resources using SpEL expressions:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     control-flow:
 *       properties:
 *         '[ows.global]': "${cpu.cores} * 2"
 *         '[ows.wms.getmap]': "${cpu.cores}"
 * }</pre>
 *
 * <p>The initializer is registered via {@code META-INF/spring.factories} and runs automatically
 * during application startup.
 *
 * @see ControlFlowConfigurationProperties
 * @see ExpressionEvaluator
 * @since 2.28.1.1
 */
public class ControlFlowAppContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * Initializes the {@code cpu.cores} system property if not already set.
     *
     * @param applicationContext the application context (not used, but required by interface)
     */
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String cores = System.getProperty("cpu.cores");
        if (null == cores) {
            cores = "" + Runtime.getRuntime().availableProcessors();
            System.setProperty("cpu.cores", cores);
        }
    }
}
