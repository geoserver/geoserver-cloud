/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalApplicationEventsConfiguration {

    public @Bean LocalApplicationEventPublisher localApplicationEventPublisher() {
        return new LocalApplicationEventPublisher();
    }
}
