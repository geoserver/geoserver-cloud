/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.backend.datadir;

import org.geoserver.cloud.autoconfigure.catalog.event.ConditionalOnCatalogEvents;
import org.geoserver.cloud.catalog.backend.datadir.EventualConsistencyEnforcer;
import org.geoserver.cloud.event.remote.datadir.RemoteEventDataDirectoryConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for handling remote catalog and configuration events in a distributed
 * GeoServer Cloud deployment using the data directory backend.
 *
 * <p>This configuration is activated when the data directory backend is enabled
 * ({@code geoserver.backend.data-directory.enabled=true}) and catalog events are enabled
 * (distributed event bus is active).
 *
 * <p>The {@link EventualConsistencyEnforcer} manages deferred catalog operations when events arrive
 * out of order, ensuring the catalog eventually converges to a consistent state across all nodes.
 * It is enabled by default and controlled by
 * {@code geoserver.backend.data-directory.eventual-consistency.enabled}.
 *
 * <p>The {@link RemoteEventDataDirectoryConfiguration} provides additional infrastructure for
 * processing remote catalog events specific to the data directory backend.
 *
 * <p>In a distributed deployment, catalog modification events are broadcast over a message bus to
 * synchronize all nodes. Network conditions and message delivery characteristics mean events may
 * arrive at each node in different orders. Without eventual consistency enforcement, adding a Layer
 * that references a Resource before the Resource event arrives would fail, causing catalog
 * inconsistencies across the cluster.
 *
 * @see RemoteEventDataDirectoryConfiguration
 * @see EventualConsistencyEnforcer
 * @see org.geoserver.cloud.catalog.backend.datadir.EventuallyConsistentCatalogFacade
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnDataDirectoryEnabled
@ConditionalOnCatalogEvents
@Import(RemoteEventDataDirectoryConfiguration.class)
public class RemoteEventDataDirectoryAutoConfiguration {

    /**
     * Creates the {@link EventualConsistencyEnforcer} bean that manages deferred catalog operations.
     *
     * <p>This bean is created when eventual consistency is enabled (the default). It will be injected
     * into the {@link org.geoserver.cloud.catalog.backend.datadir.EventuallyConsistentCatalogFacade}
     * to provide resilience against out-of-order event delivery.
     *
     * <p>Disable only if you have a single-node deployment or a custom event ordering guarantee.
     * Disabling in a multi-node deployment may cause catalog inconsistencies.
     *
     * @return the eventual consistency enforcer instance
     * @see org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryProperties.EventualConsistencyConfig
     */
    @Bean
    @ConditionalOnProperty(
            name = "geoserver.backend.data-directory.eventual-consistency.enabled",
            havingValue = "true",
            matchIfMissing = true)
    EventualConsistencyEnforcer eventualConsistencyEnforcer() {
        return new EventualConsistencyEnforcer();
    }
}
