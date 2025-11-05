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
 * Spring configuration that provides event processing infrastructure for the data directory backend
 * in a distributed GeoServer Cloud deployment.
 *
 * <p>This configuration creates the {@link RemoteEventDataDirectoryProcessor} bean, which serves as
 * the bridge between the distributed event bus and the local data directory catalog. When catalog
 * modifications occur on any node in the cluster, they are broadcast as events that this processor
 * receives and applies to the local catalog.
 *
 * <p><b>Key responsibilities:</b>
 * <ul>
 *   <li>Creates the event processor that listens for remote catalog and configuration events
 *   <li>Wires the processor to both the catalog facade (for catalog objects) and GeoServer facade
 *       (for configuration objects like services and settings)
 *   <li>Ensures the processor uses the raw catalog, which may be wrapped by eventual consistency
 *       enforcement if enabled
 * </ul>
 *
 * <p><b>Integration with eventual consistency:</b> The processor passes objects to the catalog
 * facade, which may be wrapped by {@link org.geoserver.cloud.catalog.backend.datadir.EventuallyConsistentCatalogFacade}
 * if eventual consistency is enabled. This allows operations with unresolved references to be
 * deferred automatically until dependencies arrive.
 *
 * @see RemoteEventDataDirectoryProcessor
 * @see org.geoserver.cloud.catalog.backend.datadir.EventuallyConsistentCatalogFacade
 * @see org.geoserver.cloud.autoconfigure.catalog.backend.datadir.RemoteEventDataDirectoryAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
public class RemoteEventDataDirectoryConfiguration {

    /**
     * Creates the event processor that synchronizes the local catalog with remote events.
     *
     * <p>The processor receives events from the message bus and applies them to the local data
     * directory catalog. It operates on the "raw" catalog, which may be wrapped by eventual
     * consistency enforcement if enabled via configuration.
     *
     * @param configFacade the GeoServer configuration facade for applying service and settings changes
     * @param rawCatalog the raw catalog plugin, potentially wrapped by eventual consistency enforcement
     * @return the remote event processor instance
     */
    @Bean
    RemoteEventDataDirectoryProcessor dataDirectoryRemoteEventProcessor(
            @Qualifier("geoserverFacade") RepositoryGeoServerFacade configFacade,
            @Qualifier("rawCatalog") CatalogPlugin rawCatalog) {
        return new RemoteEventDataDirectoryProcessor(configFacade, rawCatalog);
    }
}
