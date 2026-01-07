/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import static org.geoserver.cloud.autoconfigure.extensions.controlflow.ControlFlowConfigurationProperties.USE_PROPERTIES_FILE;

import java.util.Optional;
import java.util.Properties;
import org.geoserver.flow.ControlFlowCallback;
import org.geoserver.flow.ControlFlowConfigurator;
import org.geoserver.flow.ControlModuleStatus;
import org.geoserver.flow.DefaultFlowControllerProvider;
import org.geoserver.flow.FlowControllerProvider;
import org.geoserver.flow.config.DefaultControlFlowConfigurator;
import org.geoserver.flow.controller.IpBlacklistFilter;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the GeoServer Control-Flow extension.
 *
 * <p>The Control-Flow extension throttles incoming requests to prevent server overload and ensure
 * fair resource distribution. It queues excess requests rather than rejecting them, helping achieve
 * optimal throughput and preventing OutOfMemoryErrors.
 *
 * <p>This configuration supports two mutually exclusive modes:
 *
 * <ul>
 *   <li><b>Externalized Configuration</b> (default): Uses Spring Boot properties with SpEL
 *       expression support for dynamic limits based on CPU cores
 *   <li><b>Data Directory Configuration</b>: Uses the traditional {@code control-flow.properties}
 *       file from the GeoServer data directory
 * </ul>
 *
 * <p>The mode is controlled by {@code geoserver.extension.control-flow.use-properties-file}:
 *
 * <pre>{@code
 * # Externalized config (default)
 * geoserver.extension.control-flow.use-properties-file=false
 *
 * # Data directory config
 * geoserver.extension.control-flow.use-properties-file=true
 * }</pre>
 *
 * <p>Beans registered by this configuration:
 *
 * <ul>
 *   <li>{@link ControlFlowCallback} - Dispatcher callback that enforces flow control rules
 *   <li>{@link ControlFlowConfigurator} - Reads and parses configuration
 *   <li>{@link FlowControllerProvider} - Provides flow controllers based on configuration
 *   <li>{@link IpBlacklistFilter} - Filters requests from blacklisted IP addresses
 *   <li>{@link ControlModuleStatus} - Reports extension status for the REST API
 * </ul>
 *
 * @see ControlFlowConfigurationProperties
 * @see ConditionalOnControlFlow
 * @see <a href="https://docs.geoserver.org/main/en/user/extensions/controlflow/index.html">
 *     GeoServer Control Flow Documentation</a>
 * @since 2.28.1.1
 */
@AutoConfiguration
@Import({
    ControlFlowAutoConfiguration.Enabled.class,
    ControlFlowAutoConfiguration.UsingDataDirectoryConfiguration.class,
    ControlFlowAutoConfiguration.UsingExternalizedConfiguration.class
})
@EnableConfigurationProperties(ControlFlowConfigurationProperties.class)
public class ControlFlowAutoConfiguration {

    @Bean
    ControlModuleStatus controlExtension(
            ControlFlowConfigurationProperties config, Optional<ControlFlowCallback> callback) {

        ControlModuleStatus controlExtension = new ControlModuleStatus();
        controlExtension.setComponent("gs-control-flow");
        controlExtension.setEnabled(config.isEnabled() && callback.isPresent());
        controlExtension.setAvailable(callback.isPresent());
        return controlExtension;
    }

    /**
     * Sets up {@link ControlFlowConfigurator} and {@link FlowControllerProvider} when {@code geoserver.extension.control-flow.use-properties-file=true}
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnControlFlow
    @ConditionalOnProperty(name = USE_PROPERTIES_FILE, havingValue = "true", matchIfMissing = false)
    static class UsingDataDirectoryConfiguration {

        /**
         * Parameter {@code loader} added because {@link DefaultControlFlowConfigurator} calls {@code GeoServerExtensions.bean(GeoServerResourceLoader.class)}
         */
        @Bean
        ControlFlowConfigurator dataDirectoryPropertiesFileControlFlowConfigurator(GeoServerResourceLoader loader) {
            return new DefaultControlFlowConfigurator();
        }

        @Bean
        FlowControllerProvider defaultFlowControllerProvider(ControlFlowConfigurator configurator) {
            return new DefaultFlowControllerProvider(configurator);
        }
    }

    /**
     * Sets up {@link ControlFlowConfigurator} and {@link FlowControllerProvider} when {@code geoserver.extension.control-flow.use-properties-file=false} (default)
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnControlFlow
    @ConditionalOnProperty(name = USE_PROPERTIES_FILE, havingValue = "false", matchIfMissing = true)
    static class UsingExternalizedConfiguration {

        @Bean
        PropertiesControlFlowConfigurator externalizedControlFlowConfigurator(
                ControlFlowConfigurationProperties config) {
            return new PropertiesControlFlowConfigurator(config.resolvedProperties());
        }

        @Bean
        FlowControllerProvider defaultFlowControllerProvider(PropertiesControlFlowConfigurator configurator) {
            DefaultFlowControllerProvider provider = new DefaultFlowControllerProvider(configurator);
            configurator.setStale(false);
            return provider;
        }
    }

    // from applicationContext.xml:
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnControlFlow
    static class Enabled {
        /**
         * Parameters {@code provider} and {@code configurator} added to ensure they're
         * created before
         * {@link ControlFlowCallback#setApplicationContext(org.springframework.context.ApplicationContext)}
         * tries to register them itself.
         */
        @Bean
        ControlFlowCallback controlFlowCallback(FlowControllerProvider provider, ControlFlowConfigurator configurator) {
            return new ControlFlowCallback();
        }

        @Bean
        @ConditionalOnControlFlow
        IpBlacklistFilter ipBlacklistFilter(ControlFlowConfigurationProperties config) {
            Properties properties = config.resolvedProperties();
            return new IpBlacklistFilter(properties);
        }
    }
}
