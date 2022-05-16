/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.integration.cluster;

import org.gwc.tiling.cluster.ClusteringCacheJobManager;
import org.gwc.tiling.cluster.RemoteJobRegistry;
import org.gwc.tiling.cluster.event.CacheJobEvent;
import org.gwc.tiling.integration.local.GeoWebCacheJobsConfiguration;
import org.gwc.tiling.service.CacheJobManagerImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@Import(GeoWebCacheJobsConfiguration.class)
public class ClusteringGeoWebCacheJobsConfiguration {

    public static final String INSTANCE_ID_SUPPLIER_BEAN_NAME = "gwcTilingServiceInstanceId";

    @Bean
    @Primary
    public ClusteringCacheJobManager clusteringCacheJobManager( //
            CacheJobManagerImpl localManager, //
            RemoteJobRegistry remoteJobRegistry, //
            @Qualifier(INSTANCE_ID_SUPPLIER_BEAN_NAME) Supplier<String> instanceId, //
            ApplicationEventPublisher springEventPublisher) {

        Consumer<? super CacheJobEvent> eventPublisher = springEventPublisher::publishEvent;

        return new ClusteringCacheJobManager(
                instanceId, eventPublisher, localManager, remoteJobRegistry);
    }

    @Bean
    RemoteJobRegistry remoteJobRegistry() {
        return new RemoteJobRegistry();
    }

    @Bean(name = ClusteringGeoWebCacheJobsConfiguration.INSTANCE_ID_SUPPLIER_BEAN_NAME)
    Supplier<String> defaultGwcTilingServiceInstanceId(Environment env) {
        return () -> env.getProperty("info.instance-id");
    }
}
