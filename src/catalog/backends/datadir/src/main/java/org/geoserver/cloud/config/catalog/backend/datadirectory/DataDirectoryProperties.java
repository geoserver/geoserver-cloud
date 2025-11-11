/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.datadirectory;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import lombok.NonNull;
import org.geoserver.cloud.autoconfigure.catalog.backend.datadir.DataDirectoryAutoConfiguration;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to use GeoServer's traditional file-system based data directory as the
 * {@link GeoServerBackendConfigurer catalog and configuration backend} through the {@link
 * DataDirectoryAutoConfiguration} auto-configuration.
 */
@ConfigurationProperties(prefix = "geoserver.backend.data-directory")
@Data
public class DataDirectoryProperties {

    /** Whether the data directory backend is enabled */
    private boolean enabled;

    /** File system path to the data directory */
    private Path location;

    /** Whether to use parallel loading for faster startup. Default: true */
    private boolean parallelLoader = true;

    /** Eventual consistency enforcement configuration for distributed event handling */
    private DataDirectoryProperties.EventualConsistencyConfig eventualConsistency = new EventualConsistencyConfig();

    public @NonNull Path dataDirectory() {
        Path path = getLocation();
        Objects.requireNonNull(path, "geoserver.backend.data-directory.location config property resolves to null");
        return path;
    }

    /**
     * Configuration for eventual consistency enforcement when processing distributed catalog events.
     *
     * <p>In a distributed deployment, catalog modification events broadcast over the message bus may
     * arrive at each node in a different order than they were generated due to network conditions.
     * This configuration controls how the catalog handles out-of-order events where an object
     * references another object not yet present locally.
     *
     * <p><b>When disabled:</b> Operations with missing references will fail immediately, potentially
     * causing catalog inconsistencies across nodes.
     *
     * <p><b>When enabled:</b> Operations with missing references are deferred until dependencies
     * arrive, and query operations implement retry logic to allow brief wait periods for convergence.
     *
     * @see org.geoserver.cloud.catalog.backend.datadir.EventuallyConsistentCatalogFacade
     * @see org.geoserver.cloud.catalog.backend.datadir.EventualConsistencyEnforcer
     */
    @Data
    public static class EventualConsistencyConfig {
        /**
         * Enable eventual consistency enforcement for resilience against out-of-order catalog events.
         *
         * <p>When enabled, catalog operations that reference objects not yet present locally will be
         * deferred until those objects arrive. This prevents catalog corruption when events arrive
         * out of sequence.
         *
         * <p>Default: {@code true} (recommended for all distributed deployments)
         */
        private boolean enabled = true;

        /**
         * Retry intervals in milliseconds for catalog query operations during REST API requests.
         *
         * <p>When a REST API query (e.g., {@code GET /rest/workspaces/foo}) returns null and there
         * are pending operations waiting to converge, the query will retry after waiting the
         * specified intervals to allow time for dependencies to arrive and operations to complete.
         *
         * <p>The list size determines the maximum number of retry attempts. Each value specifies the
         * wait time in milliseconds before that retry attempt.
         *
         * <p><b>Example:</b> {@code [25, 25, 50]} means:
         * <ul>
         *   <li>Initial query returns null
         *   <li>Wait 25ms, retry #1
         *   <li>Wait 25ms, retry #2
         *   <li>Wait 50ms, retry #3 (final attempt)
         *   <li>Total maximum wait time: 100ms
         * </ul>
         *
         * <p>Default: {@code [25, 25, 50]} - 3 retries over 100ms total
         */
        private List<Integer> retries = List.of(25, 25, 50);
    }
}
