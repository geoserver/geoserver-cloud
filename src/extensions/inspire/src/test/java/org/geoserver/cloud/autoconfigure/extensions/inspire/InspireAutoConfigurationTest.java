/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.ModuleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class InspireAutoConfigurationTest {

    private ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean("geoServer", GeoServer.class, GeoServerImpl::new)
            .withConfiguration(AutoConfigurations.of(InspireAutoConfiguration.class));

    /**
     *
     */
    @Test
    void testDisabledByDefault() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("inspireExtension")
                    .getBean("inspireExtension", ModuleStatus.class)
                    .hasFieldOrPropertyWithValue("available", true)
                    .hasFieldOrPropertyWithValue("enabled", false);
        });
    }

    @Test
    void testEnabled() {
        runner.withPropertyValues("geoserver.extension.inspire.enabled=true").run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("inspireExtension")
                    .getBean("inspireExtension", ModuleStatus.class)
                    .hasFieldOrPropertyWithValue("available", true)
                    .hasFieldOrPropertyWithValue("enabled", true);
        });
    }
}
