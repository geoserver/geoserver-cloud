/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.controlflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.geoserver.flow.ControlFlowCallback;
import org.geoserver.flow.ControlFlowConfigurator;
import org.geoserver.flow.ControlModuleStatus;
import org.geoserver.flow.DefaultFlowControllerProvider;
import org.geoserver.flow.FlowControllerProvider;
import org.geoserver.flow.config.DefaultControlFlowConfigurator;
import org.geoserver.flow.controller.IpBlacklistFilter;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ControlFlowAutoConfigurationTest {

    private ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ControlFlowAppContextInitializer())
            .withConfiguration(AutoConfigurations.of(ControlFlowAutoConfiguration.class));

    @Test
    void defaultEnabled() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(ControlFlowConfigurationProperties.class)
                .hasSingleBean(ControlModuleStatus.class)
                .getBean(ControlModuleStatus.class)
                .hasFieldOrPropertyWithValue("enabled", true)
                .hasFieldOrPropertyWithValue("available", true));
    }

    @Test
    void disabled() {
        runner.withPropertyValues("geoserver.extension.control-flow.enabled: false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(ControlFlowConfigurationProperties.class)
                        .doesNotHaveBean(ControlFlowCallback.class)
                        .hasSingleBean(ControlModuleStatus.class)
                        .getBean(ControlModuleStatus.class)
                        .hasFieldOrPropertyWithValue("enabled", false)
                        .hasFieldOrPropertyWithValue("available", false));
    }

    @Test
    void conditionalOnClass() {
        runner.withClassLoader(new FilteredClassLoader(org.geoserver.flow.ControlFlowConfigurator.class))
                .withPropertyValues("geoserver.extension.control-flow.enabled: true")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(ControlFlowConfigurationProperties.class)
                        .doesNotHaveBean(ControlFlowCallback.class)
                        .hasSingleBean(ControlModuleStatus.class)
                        .getBean(ControlModuleStatus.class)
                        .hasFieldOrPropertyWithValue("enabled", false)
                        .hasFieldOrPropertyWithValue("available", false));
    }

    @Nested
    class Enabled {

        @Test
        void requiredBeans() {
            runner.run(context -> assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(ControlFlowCallback.class)
                    .hasSingleBean(IpBlacklistFilter.class));
        }
    }

    @Nested
    class UsingExternalizedConfiguration {
        @Test
        void requiredBeans() {
            runner.run(context -> {
                assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(ControlFlowConfigurator.class)
                        .getBean(ControlFlowConfigurator.class)
                        .isInstanceOf(PropertiesControlFlowConfigurator.class)
                        .hasFieldOrPropertyWithValue("stale", false);

                assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(FlowControllerProvider.class)
                        .getBean(FlowControllerProvider.class)
                        .isInstanceOf(DefaultFlowControllerProvider.class);
            });
        }
    }

    @Nested
    class UsingDataDirectoryConfiguration {
        @AfterEach
        void after() {
            GeoServerExtensionsHelper.clear();
        }

        @Test
        void requiredBeans(@TempDir File tmpDir) {
            GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(tmpDir);
            GeoServerExtensionsHelper.singleton("resourceLoader", resourceLoader, GeoServerResourceLoader.class);

            runner.withPropertyValues("geoserver.extension.control-flow.use-properties-file: true")
                    .withBean(GeoServerResourceLoader.class, () -> resourceLoader)
                    .run(context -> {
                        assertThat(context)
                                .hasNotFailed()
                                .hasSingleBean(ControlFlowConfigurator.class)
                                .getBean(ControlFlowConfigurator.class)
                                .isInstanceOf(DefaultControlFlowConfigurator.class);

                        assertThat(context)
                                .hasNotFailed()
                                .hasSingleBean(FlowControllerProvider.class)
                                .getBean(FlowControllerProvider.class)
                                .isInstanceOf(DefaultFlowControllerProvider.class);
                    });
        }
    }
}
