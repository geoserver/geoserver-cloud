/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.config;

import org.geoserver.catalog.Info;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.springframework.context.event.EventListener;

/**
 * Marker interface in case listening for all config remote events is required (e.g. {@link
 * EventListener @EventListener(RemoteConfigEvent.class) void
 * onAllRemoteCatalogEvents(RemoteConfigEvent event)}
 */
public interface RemoteConfigEvent {
    /**
     * {@link #getObjectId() object identifier} for changes performed to the {@link GeoServerInfo
     * global config} itself (e.g. {@code updateSequence} and the like)
     */
    String GEOSERVER_ID = "geoserver";
    /**
     * {@link #getObjectId() object identifier} for changes performed to the {@link LoggingInfo}
     * config
     */
    String LOGGING_ID = "logging";

    static String resolveId(Info object) {
        if (object instanceof GeoServerInfo) return GEOSERVER_ID;
        if (object instanceof LoggingInfo) return LOGGING_ID;
        return object.getId();
    }
}
