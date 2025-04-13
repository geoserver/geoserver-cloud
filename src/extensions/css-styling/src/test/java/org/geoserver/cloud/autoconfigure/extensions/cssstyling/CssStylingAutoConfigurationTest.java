/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.cssstyling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.catalog.SLDHandler;
import org.geoserver.community.css.web.CssHandler;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.wms.DefaultWebMapService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Test suite for {@link CssStylingAutoConfiguration}
 */
class CssStylingAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withBean("extensions", GeoServerExtensions.class)
            // @ConditionalOnGeoServer
            .withBean("geoServer", GeoServer.class, GeoServerImpl::new)
            // @ConditionalOnGeoServerWMS
            .withBean("wmsServiceTarget", DefaultWebMapService.class, () -> mock(DefaultWebMapService.class))
            .withConfiguration(AutoConfigurations.of(CssStylingAutoConfiguration.class));

    @Test
    void cssHandler_no_config() {
        contextRunner.withBean("sldHandler", SLDHandler.class).run(context -> assertThat(context)
                .hasSingleBean(CssHandler.class));
    }

    @Test
    void cssHandler_enabled() {
        contextRunner
                .withBean("sldHandler", SLDHandler.class)
                .withPropertyValues("geoserver.extension.css-styling.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(CssHandler.class));
    }

    @Test
    void cssHandler_disabled_registers_module_status() {
        contextRunner
                .withBean("sldHandler", SLDHandler.class)
                .withPropertyValues("geoserver.extension.css-styling.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CssHandler.class);
                    assertThat(context).hasBean("cssDisabledModuleStatus");
                    assertThat(context).getBean("cssDisabledModuleStatus").isInstanceOf(ModuleStatusImpl.class);
                });
    }

    @Test
    void cssHandler_conditional_on_sldHandler() {
        contextRunner
                .withPropertyValues("geoserver.extension.css-styling.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SLDHandler.class);
                    assertThat(context).doesNotHaveBean(CssHandler.class);
                    assertThat(context).doesNotHaveBean("cssDisabledModuleStatus");
                });
    }
}
