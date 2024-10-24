/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.remote.lifecycle;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.event.lifecycle.ReloadEvent;
import org.geoserver.cloud.event.lifecycle.ResetEvent;
import org.geoserver.config.plugin.GeoServerImpl;
import org.springframework.context.event.EventListener;

/**
 * Listens for and processes {@link ResetEvent} and {@link ReloadEvent} events.
 *
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.event.remote.lifecycle")
public class LifecycleEventProcessor {

    private final GeoServerImpl rawGeoServer;

    /**
     * @param rawGeoServer used to reset or reload
     */
    public LifecycleEventProcessor(GeoServerImpl rawGeoServer) {
        this.rawGeoServer = rawGeoServer;
    }

    @EventListener(ResetEvent.class)
    public void onReset(ResetEvent event) {

        if (event.isRemote()) {
            log.debug("Received a remote ResetEvent, triggering a GeoServer reset ({})", event);
            rawGeoServer.reset(true);
        }
    }

    @EventListener(ReloadEvent.class)
    public void onReload(ReloadEvent event) {

        if (event.isRemote()) {
            log.debug("Received a remote ReloadEvent, triggering a GeoServer reload ({})", event);
            try {
                rawGeoServer.reload(null, true);
            } catch (Exception e) {
                log.error("Error reloading catalog: ", e);
            }
        }
    }
}
