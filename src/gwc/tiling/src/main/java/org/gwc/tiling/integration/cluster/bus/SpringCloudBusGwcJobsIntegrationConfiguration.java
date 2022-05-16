/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.integration.cluster.bus;

import lombok.NonNull;

import org.gwc.tiling.cluster.ClusteringCacheJobManager;
import org.gwc.tiling.integration.cluster.ClusteringGeoWebCacheJobsConfiguration;
import org.gwc.tiling.integration.cluster.bus.event.CacheJobRemoteEvent;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.function.Supplier;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@RemoteApplicationEventScan(basePackageClasses = {CacheJobRemoteEvent.class})
public class SpringCloudBusGwcJobsIntegrationConfiguration {

    @Bean
    RemoteCacheJobEventsBridge remoteCacheJobEventsBridge(
            @NonNull ApplicationEventPublisher eventPublisher, //
            @NonNull ClusteringCacheJobManager jobManager, //
            Destination.Factory destinationFactory) {

        return new RemoteCacheJobEventsBridge(
                eventPublisher, jobManager, destinationFactory::getDestination);
    }

    @Primary
    @Bean(name = ClusteringGeoWebCacheJobsConfiguration.INSTANCE_ID_SUPPLIER_BEAN_NAME)
    Supplier<String> gwcTilingServiceInstanceId(ServiceMatcher busServiceMatcher) {
        return busServiceMatcher::getBusId;
    }
}
