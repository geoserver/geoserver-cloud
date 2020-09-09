/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jdbcconfig.bus;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.cloud.bus.event.RemoteInfoEvent;
import org.geoserver.cloud.bus.event.RemoteModifyEvent;
import org.geoserver.cloud.bus.event.RemoteRemoveEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogEvent;
import org.geoserver.cloud.event.ConfigInfoInfoType;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.context.event.EventListener;

/**
 * Listens to {@link RemoteCatalogEvent}s and evicts the modified or deleted {@link CatalogInfo }
 * from the {@link ConfigDatabase} cache
 */
@Slf4j(topic = "org.geoserver.cloud.bus.incoming.jdbcconfig")
public class JdbcConfigRemoteEventProcessor {
    private @Autowired ServiceMatcher busServiceMatcher;

    private @Autowired ConfigDatabase jdbcConfigDatabase;

    @EventListener(RemoteRemoveEvent.class)
    public void onRemoteRemoveEvent(RemoteRemoveEvent<?, ? extends Info> event) {
        evictConfigDatabaseEntry(event);
    }

    @EventListener(RemoteModifyEvent.class)
    public void onRemoteModifyEvent(RemoteModifyEvent<?, ? extends Info> event) {
        evictConfigDatabaseEntry(event);
    }

    private void evictConfigDatabaseEntry(RemoteInfoEvent<?, ? extends Info> event) {
        if (busServiceMatcher.isFromSelf(event)) {
            return;
        }
        ConfigInfoInfoType infoType = event.getInfoType();
        if (ConfigInfoInfoType.Catalog.equals(infoType)) {
            log.trace(
                    "ignore catalog default workspace or default namespace change event, no need to treat it.");
        } else {
            log.debug("Evict JDBCConfig cache for {}", event);
            String catalogInfoId = event.getObjectId();
            jdbcConfigDatabase.clearCacheIfPresent(catalogInfoId);
        }
    }
}
