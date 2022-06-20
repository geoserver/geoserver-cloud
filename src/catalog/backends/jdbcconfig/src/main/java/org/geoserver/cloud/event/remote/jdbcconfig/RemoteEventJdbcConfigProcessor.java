/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.remote.jdbcconfig;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.info.InfoModifyEvent;
import org.geoserver.cloud.event.info.InfoRemoveEvent;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

/**
 * Listens to {@link RemoteCatalogEvent}s and evicts the modified or deleted {@link CatalogInfo }
 * from the {@link ConfigDatabase} cache
 */
@Slf4j(topic = "org.geoserver.cloud.bus.incoming.jdbcconfig")
public class RemoteEventJdbcConfigProcessor {
    private @Autowired ConfigDatabase jdbcConfigDatabase;

    @EventListener(InfoRemoveEvent.class)
    public void onRemoteRemoveEvent(InfoRemoveEvent<?, ?> event) {
        evictConfigDatabaseEntry(event);
    }

    @EventListener(InfoModifyEvent.class)
    public void onRemoteModifyEvent(InfoModifyEvent<?, ? extends Info> event) {
        evictConfigDatabaseEntry(event);
    }

    private void evictConfigDatabaseEntry(InfoEvent<?, ?> event) {
        event.remote()
                .ifPresent(
                        remoteEvent -> {
                            ConfigInfoType infoType = event.getObjectType();
                            if (ConfigInfoType.Catalog.equals(infoType)) {
                                log.trace(
                                        "ignore catalog default workspace or default namespace change event, no need to treat it.");
                            } else {
                                log.debug("Evict JDBCConfig cache for {}", event);
                                String catalogInfoId = event.getObjectId();
                                jdbcConfigDatabase.clearCacheIfPresent(catalogInfoId);
                            }
                        });
    }
}
