/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig;

import org.geoserver.cloud.autoconfigure.catalog.event.ConditionalOnCatalogEvents;
import org.geoserver.cloud.event.remote.jdbcconfig.RemoteEventJdbcConfigProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnJdbcConfigEnabled
@ConditionalOnCatalogEvents
public class RemoteEventJdbcConfigAutoConfiguration {

    @Bean
    public RemoteEventJdbcConfigProcessor jdbcConfigRemoteEventProcessor() {
        return new RemoteEventJdbcConfigProcessor();
    }
}
