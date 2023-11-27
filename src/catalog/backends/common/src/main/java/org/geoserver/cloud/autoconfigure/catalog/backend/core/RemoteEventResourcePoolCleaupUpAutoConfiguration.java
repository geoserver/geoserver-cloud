/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.core;

import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.autoconfigure.catalog.event.ConditionalOnCatalogEvents;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.remote.resourcepool.RemoteEventResourcePoolProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Cleans up cached {@link ResourcePool} entries upon remote Catalog events
 *
 * @since 1.0
 */
@AutoConfiguration
@ConditionalOnClass(InfoEvent.class)
@ConditionalOnCatalogEvents
public class RemoteEventResourcePoolCleaupUpAutoConfiguration {

    @Bean
    RemoteEventResourcePoolProcessor remoteEventResourcePoolProcessor(
            @Qualifier("rawCatalog") CatalogPlugin rawCatalog) {

        return new RemoteEventResourcePoolProcessor(rawCatalog);
    }
}
