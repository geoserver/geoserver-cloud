/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.bus;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.security.RolesChanged;
import org.geoserver.cloud.event.security.SecurityConfigChanged;
import org.geoserver.cloud.event.security.UsersAndGroupsChanged;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Verifies the security events published by one service instance reach the other instances through the event bus with
 * their payload intact.
 */
class SecurityRemoteApplicationEventsIT extends BusAmqpIntegrationTests {

    private @Autowired ApplicationEventPublisher localEventPublisher;

    @Test
    void usersAndGroupsChangedReachesTheRemoteService() {
        eventsCaptor.stop().clear().captureEventsOf(UsersAndGroupsChanged.class).start();

        localEventPublisher.publishEvent(UsersAndGroupsChanged.createLocal("default"));

        UsersAndGroupsChanged sent = expectSent(UsersAndGroupsChanged.class);
        assertThat(sent.getServiceName()).isEqualTo("default");

        UsersAndGroupsChanged received = expectReceived(UsersAndGroupsChanged.class);
        assertThat(received.getServiceName()).isEqualTo("default");
    }

    @Test
    void rolesChangedReachesTheRemoteService() {
        eventsCaptor.stop().clear().captureEventsOf(RolesChanged.class).start();

        localEventPublisher.publishEvent(RolesChanged.createLocal("default"));

        RolesChanged sent = expectSent(RolesChanged.class);
        assertThat(sent.getServiceName()).isEqualTo("default");

        RolesChanged received = expectReceived(RolesChanged.class);
        assertThat(received.getServiceName()).isEqualTo("default");
    }

    @Test
    void securityConfigChangedReachesTheRemoteService() {
        eventsCaptor.stop().clear().captureEventsOf(SecurityConfigChanged.class).start();

        localEventPublisher.publishEvent(SecurityConfigChanged.createLocal(1002L, "SecurityManagerConfig changed"));

        SecurityConfigChanged sent = expectSent(SecurityConfigChanged.class);
        assertThat(sent.getReason()).isEqualTo("SecurityManagerConfig changed");

        SecurityConfigChanged received = expectReceived(SecurityConfigChanged.class);
        assertThat(received.getReason()).isEqualTo("SecurityManagerConfig changed");
        assertThat(received.getUpdateSequence()).isEqualTo(1002L);
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
