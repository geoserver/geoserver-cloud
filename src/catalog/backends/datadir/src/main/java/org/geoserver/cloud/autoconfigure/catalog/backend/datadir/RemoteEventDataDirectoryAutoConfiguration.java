/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.datadir;

import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.autoconfigure.catalog.event.ConditionalOnCatalogEvents;
import org.geoserver.cloud.event.remote.datadir.RemoteEventDataDirectoryProcessor;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnDataDirectoryEnabled
@ConditionalOnCatalogEvents
public class RemoteEventDataDirectoryAutoConfiguration {

    public @Bean RemoteEventDataDirectoryProcessor dataDirectoryRemoteEventProcessor(
            @Qualifier("geoserverFacade") RepositoryGeoServerFacade configFacade,
            @Qualifier("catalogFacade") ExtendedCatalogFacade catalogFacade) {
        return new RemoteEventDataDirectoryProcessor(configFacade, catalogFacade);
    }
}
