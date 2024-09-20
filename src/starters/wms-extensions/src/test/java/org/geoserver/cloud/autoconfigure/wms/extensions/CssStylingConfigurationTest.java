/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.wms.extensions;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.catalog.SLDHandler;
import org.geoserver.community.css.web.CssHandler;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatusImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Test suite for {@link CssStylingConfiguration}
 *
 * @since 1.0
 */
class CssStylingConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withBean("extensions", GeoServerExtensions.class)
                    .withConfiguration(AutoConfigurations.of(CssStylingConfiguration.class));

    @Test
    void cssHandler_no_config() {
        contextRunner
                .withBean("sldHandler", SLDHandler.class)
                .run(context -> assertThat(context).hasSingleBean(CssHandler.class));
    }

    @Test
    void cssHandler_enabled() {
        contextRunner
                .withBean("sldHandler", SLDHandler.class)
                .withPropertyValues("geoserver.styling.css.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(CssHandler.class));
    }

    @Test
    void cssHandler_disabled_registers_module_status() {
        contextRunner
                .withBean("sldHandler", SLDHandler.class)
                .withPropertyValues("geoserver.styling.css.enabled=false")
                .run(
                        context -> {
                            assertThat(context).doesNotHaveBean(CssHandler.class);
                            assertThat(context).hasBean("cssDisabledModuleStatus");
                            assertThat(context)
                                    .getBean("cssDisabledModuleStatus")
                                    .isInstanceOf(ModuleStatusImpl.class);
                        });
    }

    @Test
    void cssHandler_conditional_on_sldHandler() {
        contextRunner
                .withPropertyValues("geoserver.styling.css.enabled=true")
                .run(
                        context -> {
                            assertThat(context).doesNotHaveBean(SLDHandler.class);
                            assertThat(context).doesNotHaveBean(CssHandler.class);
                            assertThat(context).doesNotHaveBean("cssDisabledModuleStatus");
                        });
    }
}
