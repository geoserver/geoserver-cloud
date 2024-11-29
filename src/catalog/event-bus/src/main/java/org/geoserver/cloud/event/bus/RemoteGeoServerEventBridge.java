/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.bus;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.info.InfoEvent;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Listens to local catalog and configuration change {@link InfoEvent}s produced by this service
 * instance and broadcasts them to the cluster as {@link RemoteGeoServerEvent}, and conversely,
 * listens to incoming {@link RemoteGeoServerEvent}s and publishes their {@link
 * RemoteGeoServerEvent#getEvent() event} payload as local events
 *
 * @see #publishRemoteEvent(GeoServerEvent)
 * @see #publishLocalEvent(RemoteGeoServerEvent)
 */
@Slf4j(topic = "org.geoserver.cloud.event.bus.bridge")
public class RemoteGeoServerEventBridge implements DisposableBean {

    /**
     * Provided event publisher for incoming remote events converted to local events (e.g. {@link
     * ApplicationEventPublisher#publishEvent})
     *
     * @see #publishRemoteEvent(GeoServerEvent)
     */
    private final Consumer<GeoServerEvent> inboundEventPublisher;

    /**
     * Provided event publisher for outgoing remote events converted from local events (e.g. {@link
     * ApplicationEventPublisher#publishEvent})
     *
     * @see #publishLocalEvent(RemoteGeoServerEvent)
     */
    private final Consumer<RemoteGeoServerEvent> outboundEventPublisher;

    private final RemoteGeoServerEventMapper mapper;

    private final AtomicBoolean enabled = new AtomicBoolean(false);

    public RemoteGeoServerEventBridge( //
            @NonNull Consumer<GeoServerEvent> localRemoteEventPublisher, //
            @NonNull Consumer<RemoteGeoServerEvent> remoteEventPublisher, //
            @NonNull RemoteGeoServerEventMapper mapper) {

        this.mapper = mapper;
        this.outboundEventPublisher = remoteEventPublisher;
        this.inboundEventPublisher = localRemoteEventPublisher;
        enable();
    }

    @VisibleForTesting
    void enable() {
        if (enabled.compareAndSet(false, true)) {
            log.debug("RemoteGeoServerEventBridge enabled");
        }
    }

    @VisibleForTesting
    void disable() {
        if (enabled.compareAndSet(true, false)) {
            log.debug("RemoteGeoServerEventBridge disabled");
        }
    }

    @Override
    public void destroy() {
        log.info("RemoteGeoServerEventBridge received destroy signal, stopping remote event processing");
        disable();
    }

    private boolean enabled() {
        return enabled.get();
    }

    /**
     * Highest priority listener for incoming {@link RemoteGeoServerEvent} events to resolve the
     * payload {@link CatalogInfo} properties, as they may come either as {@link ResolvingProxy}
     * proxies, or {@code null} in case of collection properties.
     */
    @EventListener(RemoteGeoServerEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void publishLocalEvent(RemoteGeoServerEvent busEvent) {
        mapper.ifRemote(busEvent)
                .ifPresentOrElse(
                        incoming -> {
                            logReceived(incoming);
                            dispatchAccepted(incoming);
                        },
                        () -> logIgnoreLocalRemote(busEvent));
    }

    /**
     * Lowest priority listener on a local {@link GeoServerEvent}, publishes a matching {@link
     * RemoteGeoServerEvent} to the event bus
     */
    @EventListener(GeoServerEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void publishRemoteEvent(GeoServerEvent event) {
        mapper.mapIfLocal(event).ifPresentOrElse(this::dispatchAccepted, () -> logIgnoreRemoteLocal(event));
    }

    private void dispatchAccepted(RemoteGeoServerEvent event) {
        if (enabled()) {
            if (event.getEvent().isLocal()) {
                doSend(event);
            } else {
                doReceive(event);
            }
        }
    }

    private void doSend(RemoteGeoServerEvent outgoing) {
        try {
            outboundEventPublisher.accept(outgoing);
            logOutgoing(outgoing);
        } catch (RuntimeException e) {
            log.error("error broadcasting {}", outgoing, e);
            throw e;
        }
    }

    private void doReceive(RemoteGeoServerEvent incoming) {
        try {
            GeoServerEvent localRemoteEvent = mapper.toLocalRemote(incoming);
            if (log.isDebugEnabled()) log.debug("publishing as local event {}", incoming.toShortString());
            inboundEventPublisher.accept(localRemoteEvent);
        } catch (RuntimeException e) {
            log.error("{}: error accepting remote {}", mapper.localBusServiceId(), incoming, e);
            throw e;
        }
    }

    private void logIgnoreLocalRemote(RemoteGeoServerEvent incoming) {
        if (log.isTraceEnabled())
            log.trace(
                    "{}: not broadcasting local-remote event {}", mapper.localBusServiceId(), incoming.toShortString());
    }

    private void logIgnoreRemoteLocal(GeoServerEvent event) {
        log.trace("{}: not re-publishing {}", mapper.localBusServiceId(), event);
    }

    private void logReceived(RemoteGeoServerEvent incoming) {
        if (log.isDebugEnabled()) {
            log.debug("received remote event {}", incoming.toShortString());
        }
    }

    protected void logOutgoing(RemoteGeoServerEvent remoteEvent) {
        if (log.isDebugEnabled()) {
            log.debug("sent remote event {}", remoteEvent.toShortString());
        }
    }
}
