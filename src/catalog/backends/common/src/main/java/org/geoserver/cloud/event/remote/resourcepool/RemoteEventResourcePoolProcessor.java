/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.remote.resourcepool;

import static java.util.Optional.ofNullable;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.ResourcePool.CacheClearingListener;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Optional;

/**
 * Cleans up cached {@link ResourcePool} entries upon remote {@link CatalogInfoAdded}s, {@link
 * CatalogInfoModified}s, and {@link CatalogInfoRemoved}s.
 *
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.event.remote.resourcepool")
public class RemoteEventResourcePoolProcessor {

    private Catalog rawCatalog;

    /**
     * @param rawCatalog used to evict cached live data sources from its {@link
     *     Catalog#getResourcePool() ResourcePool}
     */
    public RemoteEventResourcePoolProcessor(Catalog rawCatalog) {
        this.rawCatalog = rawCatalog;
    }

    /**
     * no-op, really, what do we care if a CatalogInfo has been added until an incoming service
     * request needs it
     */
    @EventListener(CatalogInfoAdded.class)
    public void onCatalogRemoteAddEvent(CatalogInfoAdded event) {
        event.remote().ifPresent(e -> log.trace("ignoring {}", e));
    }

    @EventListener(CatalogInfoRemoved.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void onCatalogRemoteRemoveEvent(CatalogInfoRemoved event) {
        event.remote()
                .ifPresentOrElse(
                        this::evictFromResourcePool,
                        () -> log.trace("Ignoring event from self: {}", event));
    }

    @EventListener(CatalogInfoModified.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void onCatalogRemoteModifyEvent(CatalogInfoModified event) {
        event.remote()
                .ifPresentOrElse(
                        this::evictFromResourcePool,
                        () -> log.trace("Ignoring event from self: {}", event));
    }

    private void evictFromResourcePool(InfoEvent event) {
        final String id = event.getObjectId();
        final ConfigInfoType infoType = event.getObjectType();

        Optional<CatalogInfo> info;
        if (infoType.isA(StoreInfo.class)) {
            info = ofNullable(rawCatalog.getStore(id, StoreInfo.class));
        } else if (infoType.isA(ResourceInfo.class)) {
            info = ofNullable(rawCatalog.getResource(id, ResourceInfo.class));
        } else if (infoType.isA(StyleInfo.class)) {
            info = ofNullable(rawCatalog.getStyle(id));
        } else {
            log.trace("no need to clear resource pool cache entry for object of type {}", infoType);
            return;
        }

        info.ifPresentOrElse(
                object -> {
                    log.debug(
                            "Evicting ResourcePool cache entry for {}({}) upon {}",
                            infoType,
                            id,
                            event.toShortString());
                    ResourcePool resourcePool = rawCatalog.getResourcePool();
                    CacheClearingListener cleaner = new CacheClearingListener(resourcePool);
                    object.accept(cleaner);
                }, //
                () ->
                        log.debug(
                                "{}({}) not found, unable to clean up its ResourcePool cache entry",
                                infoType,
                                id));
    }
}
