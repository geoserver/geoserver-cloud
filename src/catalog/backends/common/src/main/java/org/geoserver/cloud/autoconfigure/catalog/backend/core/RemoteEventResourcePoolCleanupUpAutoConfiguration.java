/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.core;

import org.geoserver.catalog.ResourcePool;
import org.geoserver.cloud.autoconfigure.catalog.event.ConditionalOnCatalogEvents;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.lifecycle.LifecycleEvent;
import org.geoserver.cloud.event.remote.resourcepool.RemoteEventResourcePoolProcessor;
import org.geoserver.config.plugin.GeoServerImpl;
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
@ConditionalOnClass(value = {InfoEvent.class, LifecycleEvent.class})
@ConditionalOnCatalogEvents
public class RemoteEventResourcePoolCleanupUpAutoConfiguration {

    @Bean
    RemoteEventResourcePoolProcessor remoteEventResourcePoolProcessor(
            @Qualifier("geoServer") GeoServerImpl rawGeoServer) {

        return new RemoteEventResourcePoolProcessor(rawGeoServer);
    }
}
