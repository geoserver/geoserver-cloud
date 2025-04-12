/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.remote.datadir;

import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to contribute beans related to handling remotely produced catalog and config events
 */
@Configuration(proxyBeanMethods = false)
public class RemoteEventDataDirectoryConfiguration {

    @Bean
    RemoteEventDataDirectoryProcessor dataDirectoryRemoteEventProcessor(
            @Qualifier("geoserverFacade") RepositoryGeoServerFacade configFacade,
            @Qualifier("rawCatalog") CatalogPlugin rawCatalog) {
        return new RemoteEventDataDirectoryProcessor(configFacade, rawCatalog);
    }
}
