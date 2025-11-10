/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig;

import org.geoserver.cloud.autoconfigure.catalog.event.ConditionalOnCatalogEvents;
import org.geoserver.cloud.event.remote.jdbcconfig.RemoteEventJdbcConfigProcessor;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnJdbcConfigEnabled
@ConditionalOnCatalogEvents
public class RemoteEventJdbcConfigAutoConfiguration {

    @Bean
    RemoteEventJdbcConfigProcessor jdbcConfigRemoteEventProcessor(ConfigDatabase jdbcConfigDatabase) {
        return new RemoteEventJdbcConfigProcessor(jdbcConfigDatabase);
    }
}
