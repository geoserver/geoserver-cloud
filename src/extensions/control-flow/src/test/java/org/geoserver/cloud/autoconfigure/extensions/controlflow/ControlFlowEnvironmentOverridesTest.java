/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * Pins the devops contract documented in {@code config/geoserver_control_flow.yml}: control-flow rules are map entries
 * with dotted keys, and environment variables reach them through Spring Boot's relaxed binding. The variable name's
 * tail after {@code PROPERTIES_} becomes the rule key, underscores turning back into dots, and the environment takes
 * precedence over file-defined values.
 */
class ControlFlowEnvironmentOverridesTest {

    @Test
    void environmentVariablesBindDottedRuleKeys() {
        Map<String, Object> environment = Map.of(
                "GEOSERVER_EXTENSION_CONTROLFLOW_PROPERTIES_OWS_GLOBAL", "7",
                "GEOSERVER_EXTENSION_CONTROLFLOW_PROPERTIES_OWS_WMS_GETMAP", "3");

        runnerWithEnvironment(environment).run(context -> {
            ControlFlowConfigurationProperties config = context.getBean(ControlFlowConfigurationProperties.class);
            assertThat(config.resolvedProperties())
                    .containsEntry("ows.global", "7")
                    .containsEntry("ows.wms.getmap", "3");
        });
    }

    @Test
    void environmentVariableOverridesFileDefinedRule() {
        Map<String, Object> environment = Map.of("GEOSERVER_EXTENSION_CONTROLFLOW_PROPERTIES_OWS_GLOBAL", "9");

        runnerWithEnvironment(environment)
                .withPropertyValues("geoserver.extension.control-flow.properties.[ows.global]=4")
                .run(context -> {
                    ControlFlowConfigurationProperties config =
                            context.getBean(ControlFlowConfigurationProperties.class);
                    assertThat(config.resolvedProperties()).containsEntry("ows.global", "9");
                });
    }

    /**
     * Mirrors production source ordering: the system environment sits above file-provided configuration. The source
     * must use the {@code systemEnvironment} name: Spring Boot selects the environment-variable name mapper (uppercase
     * and underscores to dotted keys) by that exact source name, and this replaces the real one, isolating the test
     * from the host's variables.
     */
    private ApplicationContextRunner runnerWithEnvironment(Map<String, Object> environment) {
        return new ApplicationContextRunner()
                .withInitializer(new ControlFlowAppContextInitializer())
                .withInitializer(context -> context.getEnvironment()
                        .getPropertySources()
                        .addFirst(new SystemEnvironmentPropertySource(
                                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, environment)))
                .withConfiguration(AutoConfigurations.of(ControlFlowAutoConfiguration.class));
    }
}
