/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.bus;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.resource.ResourceChanged;
import org.geoserver.cloud.event.resource.ResourceChanged.Change;
import org.geoserver.cloud.event.resource.ResourceChanged.Kind;
import org.geoserver.platform.resource.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/** Verifies resource change events published by one service instance reach the other instances intact. */
class ResourceRemoteApplicationEventsIT extends BusAmqpIntegrationTests {

    private @Autowired ApplicationEventPublisher localEventPublisher;

    @Test
    void resourceChangedRoundTrip() {
        eventsCaptor.stop().clear().captureEventsOf(ResourceChanged.class).start();
        List<Change> changes = List.of(new Change("workspaces/ws/styles", Kind.MODIFIED));

        localEventPublisher.publishEvent(ResourceChanged.createLocal(
                "workspaces/ws/styles/a.sld", Kind.MODIFIED, Resource.Type.RESOURCE, 1700000000000L, changes));

        ResourceChanged sent = expectSent(ResourceChanged.class);
        assertSamePayload(sent, changes);

        ResourceChanged received = expectReceived(ResourceChanged.class);
        assertSamePayload(received, changes);
    }

    @Test
    void resourceChangedWithoutChanges() {
        eventsCaptor.stop().clear().captureEventsOf(ResourceChanged.class).start();

        localEventPublisher.publishEvent(ResourceChanged.createLocal(
                "workspaces/ws/styles/a.sld", Kind.MODIFIED, Resource.Type.RESOURCE, 1700000000000L, List.of()));

        ResourceChanged received = expectReceived(ResourceChanged.class);
        assertSamePayload(received, List.of());
    }

    private void assertSamePayload(ResourceChanged event, List<Change> changes) {
        assertThat(event.getPath()).isEqualTo("workspaces/ws/styles/a.sld");
        assertThat(event.getKind()).isEqualTo(Kind.MODIFIED);
        assertThat(event.getType()).isEqualTo(Resource.Type.RESOURCE);
        assertThat(event.getMtime()).isEqualTo(1700000000000L);
        assertThat(event.getChanges()).isEqualTo(changes);
    }

    /** The event as broadcast by the local service instance */
    private <E extends GeoServerEvent> E expectSent(Class<E> type) {
        RemoteGeoServerEvent busEvent = eventsCaptor.local().expectOne(type);
        E event = type.cast(busEvent.getEvent());
        assertThat(event.isLocal()).isTrue();
        return event;
    }

    /** The event as received by the remote service instance, after a round trip through the message broker */
    private <E extends GeoServerEvent> E expectReceived(Class<E> type) {
        RemoteGeoServerEvent busEvent = eventsCaptor.remote().expectOne(type);
        E event = type.cast(busEvent.getEvent());
        assertThat(event.isRemote()).isTrue();
        return event;
    }
}
