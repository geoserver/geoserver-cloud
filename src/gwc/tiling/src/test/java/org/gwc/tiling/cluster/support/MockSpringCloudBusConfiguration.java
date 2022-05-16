/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.cluster.support;

import org.springframework.cloud.bus.ConditionalOnBusEnabled;
import org.springframework.cloud.bus.PathServiceMatcherAutoConfiguration;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.PathDestinationFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBusEnabled
@Import(PathServiceMatcherAutoConfiguration.class)
public class MockSpringCloudBusConfiguration {

    @Bean
    Destination.Factory destinationFactory() {
        return new PathDestinationFactory();
    }
}
