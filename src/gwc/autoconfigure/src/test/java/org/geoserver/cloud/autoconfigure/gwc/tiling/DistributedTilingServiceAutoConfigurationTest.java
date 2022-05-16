/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.tiling;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.gwc.tiling.cluster.ClusteringCacheJobManager;
import org.gwc.tiling.cluster.RemoteJobRegistry;
import org.gwc.tiling.cluster.support.MockSpringCloudBusConfiguration;
import org.gwc.tiling.integration.cluster.ClusteringGeoWebCacheJobsConfiguration;
import org.gwc.tiling.integration.cluster.bus.RemoteCacheJobEventsBridge;
import org.gwc.tiling.service.CacheJobManager;
import org.gwc.tiling.service.CacheJobRegistry;
import org.gwc.tiling.service.TileCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.io.File;
import java.util.function.Supplier;

/**
 * @since 1.0
 */
class DistributedTilingServiceAutoConfigurationTest {

    @TempDir File tmpDir;
    WebApplicationContextRunner runner;

    @BeforeEach
    void setUp() throws Exception {
        runner =
                GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(tmpDir)
                        .withUserConfiguration(MockSpringCloudBusConfiguration.class)
                        .withConfiguration(
                                AutoConfigurations.of(
                                        TilingServiceAutoConfiguration.class,
                                        DistributedTilingServiceAutoConfiguration.class));
    }

    @Test
    void testConditionalOnGeoWebCacheEnabled() {
        runner.withPropertyValues("gwc.enabled=false", "spring.cloud.bus.enabled=true")
                .run(
                        context -> {
                            assertThat(context).doesNotHaveBean(CacheJobRegistry.class);
                            assertThat(context).doesNotHaveBean(CacheJobManager.class);
                            assertThat(context).doesNotHaveBean(TileCacheManager.class);
                        });
    }

    @Test
    void testConditionalOnBusEnabled() {
        runner.withPropertyValues("gwc.enabled=true", "spring.cloud.bus.enabled=false")
                .run(
                        context -> {
                            assertThat(context)
                                    .hasBean("localCacheJobManager")
                                    .isNotInstanceOf(ClusteringCacheJobManager.class);
                            assertThat(context).hasSingleBean(CacheJobRegistry.class);
                            assertThat(context).hasSingleBean(TileCacheManager.class);

                            assertThat(context).doesNotHaveBean(ClusteringCacheJobManager.class);
                            assertThat(context).doesNotHaveBean(RemoteJobRegistry.class);
                            assertThat(context).doesNotHaveBean(RemoteCacheJobEventsBridge.class);
                        });
    }

    @Test
    void testContext() {
        runner.run(
                context -> {
                    assertThat(context)
                            .getBean(CacheJobManager.class)
                            .isInstanceOf(ClusteringCacheJobManager.class);
                    assertThat(context).hasSingleBean(CacheJobRegistry.class);
                    assertThat(context).hasSingleBean(TileCacheManager.class);

                    assertThat(context).hasSingleBean(ClusteringCacheJobManager.class);
                    assertThat(context).hasSingleBean(RemoteJobRegistry.class);
                    assertThat(context).hasSingleBean(RemoteCacheJobEventsBridge.class);
                });
    }

    @Test
    void testInstanceIdBusEnabled() {
        runner.withPropertyValues("spring.cloud.bus.id=test-bus-id")
                .run(
                        context -> {
                            Object bean =
                                    context.getBean(
                                            ClusteringGeoWebCacheJobsConfiguration
                                                    .INSTANCE_ID_SUPPLIER_BEAN_NAME);
                            assertThat(bean).isInstanceOf(Supplier.class);
                            @SuppressWarnings("unchecked")
                            Supplier<String> supplier = (Supplier<String>) bean;
                            assertThat(supplier.get()).isEqualTo("test-bus-id");
                        });
    }
}
