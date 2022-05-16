/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.tiling;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.gwc.tiling.service.CacheJobManager;
import org.gwc.tiling.service.CacheJobManagerImpl;
import org.gwc.tiling.service.CacheJobRegistry;
import org.gwc.tiling.service.TileCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.io.File;

/**
 * @since 1.0
 */
class TilingServiceAutoConfigurationTest {

    @TempDir File tmpDir;
    WebApplicationContextRunner runner;

    @BeforeEach
    void setUp() throws Exception {
        runner =
                GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(tmpDir)
                        .withConfiguration(
                                AutoConfigurations.of(TilingServiceAutoConfiguration.class));
    }

    @Test
    void testContext() {
        runner.run(
                context -> {
                    assertThat(context).hasSingleBean(CacheJobRegistry.class);
                    assertThat(context).hasSingleBean(CacheJobManager.class);
                    assertThat(context)
                            .getBean(CacheJobManager.class)
                            .isInstanceOf(CacheJobManagerImpl.class);
                    assertThat(context).hasSingleBean(TileCacheManager.class);
                });
    }

    @Test
    void testConditionalOnGeoWebCacheEnabled() {
        runner.withPropertyValues("gwc.enabled=false")
                .run(
                        context -> {
                            assertThat(context).doesNotHaveBean(CacheJobRegistry.class);
                            assertThat(context).doesNotHaveBean(CacheJobManager.class);
                            assertThat(context).doesNotHaveBean(TileCacheManager.class);
                        });
    }
}
