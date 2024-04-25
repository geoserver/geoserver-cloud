/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */

package org.geoserver.cloud.autoconfigure.wms.extensions;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.platform.ModuleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Test suite for {@link GraticuleConfiguration}
 *
 * @since 1.8
 */
class GraticuleConfigurationTest {

    private ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(GraticuleConfiguration.class));

    @Test
    void enabledByDefault() {
        testEnabled();
    }

    @Test
    void enabledExplicitly() {
        contextRunner = contextRunner.withPropertyValues("geoserver.wms.graticule.enabled=true");
        testEnabled();
    }

    @Test
    void disabled() {
        contextRunner = contextRunner.withPropertyValues("geoserver.wms.graticule.enabled=false");
        contextRunner
                .run(
                        context ->
                                assertThat(context)
                                        .getBean(
                                                "graticuleDisabledModuleStatus", ModuleStatus.class)
                                        .hasFieldOrPropertyWithValue("enabled", false))
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(GraticuleConfiguration.Enabled.class))
                .run(context -> assertThat(context).doesNotHaveBean("graticuleStorePanel"));
    }

    void testEnabled() {
        contextRunner
                .run(
                        context ->
                                assertThat(context)
                                        .hasSingleBean(GraticuleConfiguration.Enabled.class))
                .run(context -> assertThat(context).hasBean("graticuleStorePanel"))
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean("graticuleDisabledModuleStatus"));
    }
}
