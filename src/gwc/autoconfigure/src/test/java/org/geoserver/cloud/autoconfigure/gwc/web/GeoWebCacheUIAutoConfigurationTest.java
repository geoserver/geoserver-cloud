/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geowebcache.rest.controller.ByteStreamController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class GeoWebCacheUIAutoConfigurationTest {

    @TempDir
    File tmpDir;

    WebApplicationContextRunner runner;

    // the runner selects the gwc service
    @BeforeEach
    void setUp() {
        runner = GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(tmpDir)
                .withConfiguration(AutoConfigurations.of(GeoWebCacheUIAutoConfiguration.class));
    }

    @Test
    void disabledByDefault() {
        runner.run(context -> {
            assertThat(context)
                    .doesNotHaveBean("gwcDemoUrlHandlerMapping")
                    .doesNotHaveBean("gwcRestWebUrlHandlerMapping")
                    .doesNotHaveBean(ByteStreamController.class);
        });
    }

    @Test
    void enabled() {
        runner.withPropertyValues("gwc.web-ui=true").run(context -> {
            assertThat(context)
                    .hasBean("gwcDemoUrlHandlerMapping")
                    .hasBean("gwcRestWebUrlHandlerMapping")
                    .hasSingleBean(ByteStreamController.class);
        });
    }

    @Test
    void disabledOnOtherServices() {
        runner.withPropertyValues("gwc.web-ui=true", "geoserver.services=wms").run(context -> {
            assertThat(context)
                    .doesNotHaveBean("gwcDemoUrlHandlerMapping")
                    .doesNotHaveBean("gwcRestWebUrlHandlerMapping")
                    .doesNotHaveBean(ByteStreamController.class);
        });
    }
}
