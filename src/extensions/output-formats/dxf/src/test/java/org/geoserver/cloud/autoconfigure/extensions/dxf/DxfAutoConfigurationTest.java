/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.dxf;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.wfs.response.DXFOutputFormat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Test suite for {@link DxfAutoConfiguration}
 */
class DxfAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withBean("extensions", GeoServerExtensions.class)
            .withBean("geoServer", GeoServer.class, GeoServerImpl::new)
            .withConfiguration(AutoConfigurations.of(DxfAutoConfiguration.class));

    @Test
    void testEnabledByDefault() {
        contextRunner.withPropertyValues("geoserver.service.wfs.enabled=true").run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(DXFOutputFormat.class)
                .hasBean("DxfExtension")
                .getBean("DxfExtension", ModuleStatus.class)
                .hasFieldOrPropertyWithValue("enabled", true));

        contextRunner.withPropertyValues("geoserver.service.webui.enabled=true").run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(DXFOutputFormat.class)
                .hasBean("DxfExtension")
                .getBean("DxfExtension", ModuleStatus.class)
                .hasFieldOrPropertyWithValue("enabled", true));
    }

    @Test
    void testExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("geoserver.service.wfs.enabled=true", "geoserver.extension.dxf.enabled=true")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(DXFOutputFormat.class)
                        .hasBean("DxfExtension")
                        .getBean("DxfExtension", ModuleStatus.class)
                        .hasFieldOrPropertyWithValue("enabled", true));

        contextRunner
                .withPropertyValues("geoserver.service.webui.enabled=true", "geoserver.extension.dxf.enabled=true")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(DXFOutputFormat.class)
                        .hasBean("DxfExtension")
                        .getBean("DxfExtension", ModuleStatus.class)
                        .hasFieldOrPropertyWithValue("enabled", true));
    }

    @Test
    void testDisabledConfiguration() {
        contextRunner
                .withPropertyValues("geoserver.service.wfs.enabled=true", "geoserver.extension.dxf.enabled=false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(DXFOutputFormat.class)
                        .hasBean("DxfExtension")
                        .getBean("DxfExtension", ModuleStatus.class)
                        .hasFieldOrPropertyWithValue("enabled", false));
    }
}
