/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
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
 * {@link AutoConfiguration @AutoConfiguration} to contribute beans related to handling remotely
 * @SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
 * produced catalog and config events
 *
 * @see RemoteEventDataDirectoryConfiguration
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnDataDirectoryEnabled
@ConditionalOnCatalogEvents
@Import(RemoteEventDataDirectoryConfiguration.class)
public class RemoteEventDataDirectoryAutoConfiguration {

    @Bean
    @ConditionalOnProperty(
            name = "geoserver.backend.data-directory.eventual-consistency.enabled",
            havingValue = "true",
            matchIfMissing = true)
    EventualConsistencyEnforcer eventualConsistencyEnforcer() {
        return new EventualConsistencyEnforcer();
    }
}
