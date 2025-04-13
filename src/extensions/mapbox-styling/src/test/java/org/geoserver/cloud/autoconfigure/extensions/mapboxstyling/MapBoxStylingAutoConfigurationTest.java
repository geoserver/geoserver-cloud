/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.mapboxstyling;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.catalog.SLDHandler;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
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
        contextRunner = new ApplicationContextRunner()
                .withBean("extensions", GeoServerExtensions.class)
                .withBean("geoServer", GeoServer.class, GeoServerImpl::new)
                .withBean("sldHandler", SLDHandler.class)
                .withConfiguration(AutoConfigurations.of(MapBoxStylingAutoConfiguration.class));
    }

    @Test
    void testEnabledByDefault() {
        contextRunner.run(context -> assertThat(context).hasNotFailed().hasSingleBean(MBStyleHandler.class));
    }

    @Test
    void testEnabledExplicitly() {
        contextRunner
                .withPropertyValues("geoserver.extension.mapbox-styling.enabled=true")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(MBStyleHandler.class));
    }

    @Test
    void testDisabledRegistersModuleStatus() {
        contextRunner
                .withPropertyValues("geoserver.extension.mapbox-styling.enabled=false")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean(MBStyleHandler.class)
                            .hasBean("MBStyleExtension")
                            .getBean("MBStyleExtension", ModuleStatus.class)
                            .hasFieldOrPropertyWithValue("enabled", false);
                });
    }
}
