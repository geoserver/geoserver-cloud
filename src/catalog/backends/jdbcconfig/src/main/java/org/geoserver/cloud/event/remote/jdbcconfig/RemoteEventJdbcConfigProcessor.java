/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.remote.jdbcconfig;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.info.InfoModified;
import org.geoserver.cloud.event.info.InfoRemoved;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.springframework.context.event.EventListener;

/**
 * Listens to {@link RemoteCatalogEvent}s and evicts the modified or deleted {@link CatalogInfo }
 * from the {@link ConfigDatabase} cache
 */
@Slf4j(topic = "org.geoserver.cloud.bus.incoming.jdbcconfig")
@RequiredArgsConstructor
public class RemoteEventJdbcConfigProcessor {

    private final @NonNull ConfigDatabase jdbcConfigDatabase;

    @EventListener(InfoRemoved.class)
    public void onRemoteRemoveEvent(InfoRemoved event) {
        evictConfigDatabaseEntry(event);
    }

    @EventListener(InfoModified.class)
    public void onRemoteModifyEvent(InfoModified event) {
        evictConfigDatabaseEntry(event);
    }

    private void evictConfigDatabaseEntry(InfoEvent event) {
        event.remote().ifPresent(remoteEvent -> {
            ConfigInfoType infoType = event.getObjectType();
            if (ConfigInfoType.CATALOG.equals(infoType)) {
                log.trace("ignore catalog default workspace or default namespace change event, no need to treat it.");
            } else {
                log.debug("Evict JDBCConfig cache for {}", event);
                String catalogInfoId = event.getObjectId();
                jdbcConfigDatabase.clearCacheIfPresent(catalogInfoId);
            }
        });
    }
}
