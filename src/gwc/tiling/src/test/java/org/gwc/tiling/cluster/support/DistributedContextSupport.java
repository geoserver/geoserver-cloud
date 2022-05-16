/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.cluster.support;

import lombok.Getter;
import lombok.NonNull;

import org.gwc.tiling.cluster.ClusteringCacheJobManager;
import org.gwc.tiling.integration.cluster.ClusteringGeoWebCacheJobsConfiguration;
import org.gwc.tiling.integration.cluster.bus.SpringCloudBusGwcJobsIntegrationConfiguration;
import org.gwc.tiling.model.TileLayerMockSupport;
import org.gwc.tiling.service.CacheJobRegistry;
import org.gwc.tiling.service.support.MockTileLayersTestConfiguration;
import org.gwc.tiling.service.support.TileLayerMockSupportConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @since 1.0
 */
public class DistributedContextSupport {

    private @NonNull File cacheDirectory;
    private final AtomicInteger applicationInstanceIdSequence = new AtomicInteger();

    // parent context to share some info
    private ConfigurableApplicationContext parentContext;

    public DistributedContextSupport(@NonNull File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
        this.parentContext = new AnnotationConfigApplicationContext(SharedConfig.class);
    }

    @Import(TileLayerMockSupportConfiguration.class)
    static @Configuration @Component class SharedConfig {

        private @Getter List<TestInstance> instances = new ArrayList<>();

        public @Bean List<TestInstance> geowebCacheInstances() {
            return instances;
        }
    }

    public TileLayerMockSupport mockLayers() {
        return parentContext.getBean(TileLayerMockSupport.class);
    }

    public TestInstance newInstance() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setParent(parentContext);
        context.register(
                MockTileLayersTestConfiguration.class,
                ClusteringGeoWebCacheJobsConfiguration.class,
                SpringCloudBusGwcJobsIntegrationConfiguration.class,
                SpringCloudBusMockingConfiguration.class);

        String instanceId =
                String.format("gwc.test:%d", applicationInstanceIdSequence.incrementAndGet());
        Map<String, Object> properties = new HashMap<>();
        // properties.put("info.instance-id", instanceId);
        properties.put("spring.cloud.bus.id", instanceId);
        properties.put("spring.cloud.bus.enabled", "true");

        ConfigurableEnvironment environment = context.getEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new MapPropertySource("TestProperties", properties));

        context.refresh();

        ClusteringCacheJobManager instance = context.getBean(ClusteringCacheJobManager.class);
        CacheJobRegistry registry = context.getBean(CacheJobRegistry.class);
        TestInstance testInstance = new TestInstance(context, instance, registry);

        parentContext.getBean(SharedConfig.class).getInstances().add(testInstance);

        return testInstance;
    }
}
