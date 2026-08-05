/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import org.geoserver.cloud.autoconfigure.metrics.catalog.ConditionalOnGeoServerMetricsEnabled;
import org.geoserver.cloud.autoconfigure.metrics.catalog.GeoSeverMetricsConfigProperties;
import org.geoserver.flow.ControlFlowCallback;
import org.geoserver.flow.FlowControllerProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for control-flow metrics; active when the control-flow extension
 * runs, a {@code MeterRegistry} is available, and {@literal geoserver.metrics.enabled} is not {@code false}.
 *
 * @see ControlFlowMetrics
 * @since 3.1
 */
@AutoConfiguration(
        after = {
            ControlFlowAutoConfiguration.class,
            MetricsAutoConfiguration.class,
            CompositeMeterRegistryAutoConfiguration.class
        })
@ConditionalOnControlFlow
@ConditionalOnGeoServerMetricsEnabled
@ConditionalOnBean(ControlFlowCallback.class)
@EnableConfigurationProperties(GeoSeverMetricsConfigProperties.class)
@SuppressWarnings("java:S1118")
public class ControlFlowMetricsAutoConfiguration {

    @Bean
    ControlFlowMetrics controlFlowMetrics(
            GeoSeverMetricsConfigProperties metricsConfig,
            ControlFlowCallback callback,
            FlowControllerProvider provider) {

        return new ControlFlowMetrics(callback, provider, metricsConfig.getInstanceId());
    }
}
