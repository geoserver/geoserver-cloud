/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.jdbcconfig;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.cloud.bus.catalog.CatalogRemoteEvent;
import org.geoserver.cloud.bus.catalog.CatalogRemoteModifyEvent;
import org.geoserver.cloud.bus.catalog.CatalogRemoteRemoveEvent;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.context.event.EventListener;

/**
 * Listens to {@link CatalogRemoteEvent}s and evicts the modified or deleted {@link CatalogInfo }
 * from the {@link ConfigDatabase} cache
 */
@Slf4j
public class JdbcConfigRemoteEventProcessor {
    private @Autowired ServiceMatcher busServiceMatcher;

    private @Autowired ConfigDatabase jdbcConfigDatabase;

    @EventListener(CatalogRemoteRemoveEvent.class)
    public void onCatalogRemoteRemoveEvent(CatalogRemoteRemoveEvent event) {
        if (!busServiceMatcher.isFromSelf(event)) {
            evictConfigDatabaseEntry(event);
        }
    }

    @EventListener(CatalogRemoteModifyEvent.class)
    public void onCatalogRemoteModifyEvent(CatalogRemoteModifyEvent event) {
        if (!busServiceMatcher.isFromSelf(event)) {
            evictConfigDatabaseEntry(event);
        }
    }

    private void evictConfigDatabaseEntry(CatalogRemoteEvent event) {
        if (!busServiceMatcher.isFromSelf(event)) {
            log.debug("Evict JDBCConfig cache for {}", event);
            String catalogInfoId = event.getCatalogInfoId();
            jdbcConfigDatabase.clearCacheIfPresent(catalogInfoId);
        }
    }
}
