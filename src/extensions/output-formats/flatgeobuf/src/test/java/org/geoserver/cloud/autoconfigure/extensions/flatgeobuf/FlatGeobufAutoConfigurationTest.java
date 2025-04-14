/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.flatgeobuf;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.ModuleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Tests for {@link FlatGeobufAutoConfiguration}.
 */
class FlatGeobufAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        contextRunner = new ApplicationContextRunner()
                .withBean("geoServer", GeoServer.class, GeoServerImpl::new)
                .withConfiguration(AutoConfigurations.of(FlatGeobufAutoConfiguration.class));
    }

    @Test
    void testEnabledByDefault() {
        contextRunner.run(this::assertEnabled);
    }

    @Test
    void testExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("geoserver.extension.flatgeobuf.enabled=true")
                .run(this::assertEnabled);
    }

    @Test
    void testConditionalOnGeoServerWFS() {
        contextRunner
                .withPropertyValues("geoserver.service.wfs.enabled=true")
                .run(context -> assertThat(context).hasNotFailed().hasBean("flatGeobufOutputFormat"));
    }

    @Test
    void testConditionalOnGeoServerWebUI() {
        contextRunner
                .withPropertyValues("geoserver.service.webui.enabled=true")
                .run(context -> assertThat(context).hasNotFailed().hasBean("flatGeobufOutputFormat"));
    }

    private void assertEnabled(AssertableApplicationContext context) {
        assertThat(context)
                .hasNotFailed()
                .hasBean("flatGeobufExtension")
                .hasSingleBean(FlatGeobufAutoConfiguration.class)
                .hasSingleBean(FlatGeobufConfigProperties.class)
                .getBean(FlatGeobufConfigProperties.class)
                .hasFieldOrPropertyWithValue("enabled", true);

        assertThat(context)
                .getBean("flatGeobufExtension", ModuleStatus.class)
                .hasFieldOrPropertyWithValue("module", "flatgeobuf")
                .hasFieldOrPropertyWithValue("name", "FlatGeobuf WFS Output Format")
                .hasFieldOrPropertyWithValue("available", true)
                .hasFieldOrPropertyWithValue("enabled", true);
    }

    @Test
    void testExplicitlyDisabled() {
        contextRunner
                .withPropertyValues("geoserver.service.wfs.enabled=true")
                .withPropertyValues("geoserver.extension.flatgeobuf.enabled=false")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean("flatGeobufOutputFormat")
                            .hasBean("flatGeobufExtension");
                    assertThat(context)
                            .getBean("flatGeobufExtension", ModuleStatus.class)
                            .hasFieldOrPropertyWithValue("module", "flatgeobuf")
                            .hasFieldOrPropertyWithValue("name", "FlatGeobuf WFS Output Format")
                            .hasFieldOrPropertyWithValue("available", true)
                            .hasFieldOrPropertyWithValue("enabled", false);
                });
    }
}
