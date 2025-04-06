/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.mapboxstyling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.catalog.SLDHandler;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.WMS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Test suite for {@link MapBoxStylingAutoConfiguration}
 */
class MapBoxStylingAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setup() {
        // Create a mock GeoServer instance and WMS dependencies to satisfy @ConditionalOnGeoServerWMS
        var mockGeoServer = mock(GeoServer.class);
        var mockWMS = mock(WMS.class);
        var mockWmsServiceTarget = mock(DefaultWebMapService.class);

        contextRunner = new ApplicationContextRunner()
                .withBean("extensions", GeoServerExtensions.class)
                .withBean("geoServer", GeoServer.class, () -> mockGeoServer)
                .withBean("wmsServiceTarget", DefaultWebMapService.class, () -> mockWmsServiceTarget)
                .withBean(WMS.class, () -> mockWMS)
                .withConfiguration(AutoConfigurations.of(MapBoxStylingAutoConfiguration.class));
    }

    @Test
    void stylingHandler_no_config() {
        contextRunner.withBean("sldHandler", SLDHandler.class).run(context -> assertThat(context)
                .hasSingleBean(MBStyleHandler.class));
    }

    @Test
    void stylingHandler_enabled() {
        contextRunner
                .withBean("sldHandler", SLDHandler.class)
                .withPropertyValues("geoserver.extension.mapbox-styling.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(MBStyleHandler.class));
    }

    @Test
    void stylingHandler_disabled_registers_module_status() {
        contextRunner
                .withBean("sldHandler", SLDHandler.class)
                .withPropertyValues("geoserver.extension.mapbox-styling.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MBStyleHandler.class);
                    assertThat(context).hasBean("MBStyleExtension");
                    assertThat(context).getBean("MBStyleExtension").isInstanceOf(ModuleStatusImpl.class);
                });
    }

    @Test
    void stylingHandler_conditional_on_sldHandler() {
        contextRunner
                .withPropertyValues("geoserver.extension.mapbox-styling.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SLDHandler.class);
                    assertThat(context).doesNotHaveBean(MBStyleHandler.class);
                    assertThat(context).doesNotHaveBean("MBStyleExtension");
                });
    }
}
