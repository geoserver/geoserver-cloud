/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.wms.extensions;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.catalog.SLDHandler;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.platform.ModuleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Test suite for {@link CssStylingConfiguration}
 *
 * @since 1.0
 */
public class MapBoxStylingConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withBean("sldHandler", SLDHandler.class)
                    .withConfiguration(AutoConfigurations.of(MapBoxStylingConfiguration.class));

    @Test
    void MBStyleHandler_no_config() {
        contextRunner
                .run(context -> assertThat(context).hasSingleBean(MBStyleHandler.class))
                .run(context -> assertThat(context).hasBean("MBStyleExtension"))
                .run(
                        context ->
                                assertThat(context.getBean("MBStyleExtension", ModuleStatus.class))
                                        .hasFieldOrPropertyWithValue("enabled", true));
    }

    @Test
    void MBStyleHandler_enabled() {
        contextRunner
                .withPropertyValues("geoserver.styling.mapbox.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(MBStyleHandler.class))
                .run(context -> assertThat(context).hasBean("MBStyleExtension"))
                .run(
                        context ->
                                assertThat(context.getBean("MBStyleExtension", ModuleStatus.class))
                                        .hasFieldOrPropertyWithValue("enabled", true));
    }

    @Test
    void MBStyleHandler_disabled_registers_module_status() {
        contextRunner
                .withPropertyValues("geoserver.styling.mapbox.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MBStyleHandler.class))
                .run(context -> assertThat(context).hasBean("MBStyleExtension"))
                .run(
                        context ->
                                assertThat(context.getBean("MBStyleExtension", ModuleStatus.class))
                                        .hasFieldOrPropertyWithValue("enabled", false));
    }
}
