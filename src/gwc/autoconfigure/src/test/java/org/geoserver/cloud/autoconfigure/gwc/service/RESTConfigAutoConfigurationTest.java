/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geoserver.gwc.controller.GwcUrlHandlerMapping;
import org.geoserver.gwc.layer.GWCGeoServerRESTConfigurationProvider;
import org.geowebcache.rest.controller.TileLayerController;
import org.geowebcache.rest.converter.GWCConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.io.File;

/**
 * @since 1.0
 */
class RESTConfigAutoConfigurationTest {

    @TempDir File tmpDir;
    WebApplicationContextRunner runner;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {
        runner =
                GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(tmpDir)
                        .withConfiguration(
                                AutoConfigurations.of(RESTConfigAutoConfiguration.class));
    }

    @Test
    void disabledByDefault() {
        runner.run(
                context -> {
                    assertThat(context).doesNotHaveBean(GWCConverter.class);
                    assertThat(context).doesNotHaveBean(TileLayerController.class);
                });
    }

    @Test
    void enabled() {
        runner.withPropertyValues("gwc.rest-config=true")
                .run(
                        context -> {
                            assertThat(context)
                                    .hasSingleBean(GWCConverter.class)
                                    .hasSingleBean(TileLayerController.class)
                                    .hasSingleBean(GWCGeoServerRESTConfigurationProvider.class)
                                    .hasSingleBean(GwcUrlHandlerMapping.class);
                        });
    }

    @Test
    void enabledButGwcRestJarNotInClassPath() {
        runner.withClassLoader(new FilteredClassLoader(GWCConverter.class))
                .withPropertyValues("gwc.rest-config=true")
                .run(
                        context -> {
                            assertThat(context).doesNotHaveBean(GWCConverter.class);
                            assertThat(context).doesNotHaveBean(TileLayerController.class);
                        });
    }
}
