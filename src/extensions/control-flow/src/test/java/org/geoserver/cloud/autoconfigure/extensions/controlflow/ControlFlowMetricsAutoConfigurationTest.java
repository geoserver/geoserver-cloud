/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ControlFlowMetricsAutoConfigurationTest {

    private ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ControlFlowAppContextInitializer())
            .withConfiguration(AutoConfigurations.of(
                    ControlFlowAutoConfiguration.class, ControlFlowMetricsAutoConfiguration.class));

    @Test
    void enabledByDefault() {
        runner.withBean(SimpleMeterRegistry.class).run(context -> {
            assertThat(context).hasSingleBean(ControlFlowMetrics.class);

            SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
            context.getBean(ControlFlowMetrics.class).bindTo(registry);
            assertThat(registry.find("geoserver.controlflow.requests.running").gauge())
                    .isNotNull();
        });
    }

    @Test
    void disabledWhenMetricsDisabled() {
        runner.withBean(SimpleMeterRegistry.class)
                .withPropertyValues("geoserver.metrics.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ControlFlowMetrics.class));
    }

    @Test
    void disabledWithoutMeterRegistry() {
        runner.run(context -> assertThat(context).doesNotHaveBean(ControlFlowMetrics.class));
    }

    @Test
    void disabledWhenControlFlowDisabled() {
        runner.withBean(SimpleMeterRegistry.class)
                .withPropertyValues("geoserver.extension.control-flow.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ControlFlowMetrics.class));
    }
}
