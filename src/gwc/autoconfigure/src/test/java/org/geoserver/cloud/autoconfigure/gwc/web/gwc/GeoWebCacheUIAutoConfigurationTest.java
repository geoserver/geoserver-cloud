/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.web.gwc;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geoserver.cloud.autoconfigure.web.gwc.GeoWebCacheUIAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.io.File;

public class GeoWebCacheUIAutoConfigurationTest {

    WebApplicationContextRunner runner;

    @TempDir File tmpDir;

    @BeforeEach
    void setUp() throws Exception {
        runner =
                GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(tmpDir)
                        .withPropertyValues("gwc.web-ui=true")
                        .withConfiguration(
                                AutoConfigurations.of(GeoWebCacheUIAutoConfiguration.class));
    }

    @Test
    void beansForLocalWorkspacePathsHandlingArePresent() {
        runner.run(
                context -> {
                    assertNotNull(context.getBean("gwcDemoUrlHandlerMapping"));
                    assertNotNull(context.getBean("gwcRestWebUrlHandlerMapping"));
                });
    }
}
