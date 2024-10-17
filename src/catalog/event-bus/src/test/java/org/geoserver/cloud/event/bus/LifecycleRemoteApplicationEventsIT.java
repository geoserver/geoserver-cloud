/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.bus;

import org.geoserver.cloud.event.lifecycle.LifecycleEvent;
import org.geoserver.cloud.event.lifecycle.ResetEvent;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

class LifecycleRemoteApplicationEventsIT extends BusAmqpIntegrationTests {

    @BeforeAll
    static void handleGsExtensions() {
        GeoServerExtensions gse = new GeoServerExtensions();
        gse.setApplicationContext(remoteAppContext);
    }

    @Test
    void testGeoServerHasExecutedReset() {

        this.eventsCaptor.stop().clear().captureLifecycleEventsOf(LifecycleEvent.class).start();

        Consumer<GeoServer> modifier = GeoServer::reset;
        modifier.accept(geoserver);

        RemoteGeoServerEvent localRemoteEvent =
                eventsCaptor.local().expectOneLifecycleEvent(ResetEvent.class);
        RemoteGeoServerEvent sentEvent =
                eventsCaptor.remote().expectOneLifecycleEvent(ResetEvent.class);
    }
}
