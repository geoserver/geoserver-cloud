/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.caching;

import com.google.common.base.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.bus.event.RemoteInfoEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogModifyEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogRemoveEvent;
import org.geoserver.cloud.bus.event.config.RemoteConfigModifyEvent;
import org.geoserver.cloud.bus.event.config.RemoteConfigRemoveEvent;
import org.geoserver.cloud.catalog.caching.CachingCatalogFacade;
import org.geoserver.cloud.catalog.caching.CachingGeoServerFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Component to listen to {@link RemoteInfoEvent} based hierarchy of events and evict entries from
 * {@link CachingCatalogFacade} and {@link CachingGeoServerFacade} as required by the event type and
 * the object it refers to.
 */
@Slf4j
@RequiredArgsConstructor
public @Service class RemoteEventCacheEvictor {

    private final CachingCatalogFacade catalog;
    private final CachingGeoServerFacade config;

    private @Autowired ServiceMatcher busServiceMatcher;

    @EventListener(classes = {RemoteCatalogRemoveEvent.class, RemoteCatalogModifyEvent.class})
    public void evictCatalogInfo(RemoteInfoEvent<?, ?> event) {
        if (!busServiceMatcher.isFromSelf(event)) {
            // TODO: evict defaultns/ws/datastore
            String objectId = event.getObjectId();
            evictEntry(objectId, catalog::evict);
        }
    }

    @EventListener(classes = {RemoteConfigRemoveEvent.class, RemoteConfigModifyEvent.class})
    public void evictConfigInfo(RemoteInfoEvent<?, ?> event) {
        if (!busServiceMatcher.isFromSelf(event)) {
            String objectId = event.getObjectId();
            evictEntry(objectId, config::evict);
        }
    }

    private void evictEntry(String key, Function<String, Boolean> evictor) {
        boolean evicted = evictor.apply(key);
        if (evicted) {
            log.debug("Evicted cache entry {} upon remote event", key);
        } else {
            log.trace("Remove event for {} wasn't cached");
        }
    }
}
