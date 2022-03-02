/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.catalog;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.springframework.context.event.EventListener;

/**
 * Marker interface in case listening for all catalog remote events is required (e.g. {@link
 * EventListener @EventListener(RemoteCatalogEvent.class) void
 * onAllRemoteCatalogEvents(RemoteCatalogEvent event)}
 */
public interface RemoteCatalogEvent {
    /**
     * {@link #getObjectId() object identifier} for changes performed to the {@link Catalog} itself
     * (e.g. {@code defaultWorkspace} and the like)
     */
    String CATALOG_ID = "catalog";

    static String resolveId(CatalogInfo object) {
        if (object instanceof Catalog) return CATALOG_ID;
        return object == null ? null : object.getId();
    }
}
