/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.tiling;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.gwc.tiling.integration.cluster.ClusteringGeoWebCacheJobsConfiguration;
import org.gwc.tiling.integration.cluster.bus.SpringCloudBusGwcJobsIntegrationConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.bus.ConditionalOnBusEnabled;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnGeoWebCacheEnabled
@ConditionalOnClass(ClusteringGeoWebCacheJobsConfiguration.class)
@ConditionalOnBusEnabled
@AutoConfigureAfter(TilingServiceAutoConfiguration.class)
@Import({
    ClusteringGeoWebCacheJobsConfiguration.class,
    SpringCloudBusGwcJobsIntegrationConfiguration.class
})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.tiling")
public class DistributedTilingServiceAutoConfiguration {

    public @PostConstruct void log() {
        log.info("GeoWebCache core GeoServer integration enabled");
    }
}
