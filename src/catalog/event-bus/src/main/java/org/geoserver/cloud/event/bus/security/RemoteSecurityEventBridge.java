/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.bus.security;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.event.bus.catalog.RemoteInfoEvent;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.security.SecurityConfigChanged;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

/**
 * Listens to local catalog and configuration change {@link InfoEvent}s produced by this service
 * instance and broadcasts them to the cluster as {@link RemoteInfoEvent}
 */
@Slf4j(topic = "org.geoserver.cloud.event.bus.security")
public class RemoteSecurityEventBridge {

    private @Autowired ServiceMatcher serviceMatcher;
    private @Autowired ApplicationEventPublisher eventPublisher;

    @EventListener(SecurityConfigChanged.class)
    public void handleLocalEvent(SecurityConfigChanged event) {
        event.local()
                .ifPresent(
                        localEvent -> {
                            RemoteSecurityConfigEvent remoteEvent =
                                    new RemoteSecurityConfigEvent(
                                            event, this, serviceMatcher.getBusId());

                            event.setId(remoteEvent.getId());
                            event.setOrigin(serviceMatcher.getBusId());
                            log.debug("broadcasting {}", remoteEvent);
                            eventPublisher.publishEvent(remoteEvent);
                        });
    }

    @EventListener(RemoteSecurityConfigEvent.class)
    public void handleRemoteEvent(RemoteSecurityConfigEvent incoming) {
        if (serviceMatcher.isForSelf(incoming) && !serviceMatcher.isFromSelf(incoming)) {
            SecurityConfigChanged event = incoming.getEvent();
            event.setRemote(true);
            log.debug("publishing remote event to local bus {}", event);
            eventPublisher.publishEvent(event);
        }
    }
}
