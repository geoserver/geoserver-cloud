/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.bus;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogException;
import org.geoserver.cloud.gwc.event.GeoWebCacheEvent;
import org.mapstruct.factory.Mappers;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

/**
 * Listens to local {@link GeoWebCacheEvent}s produced by this service instance and broadcasts them
 * to the cluster as {@link RemoteGeoWebCacheEvent} on the event bus, and vice-versa.
 */
@RequiredArgsConstructor
@Slf4j(topic = "org.geoserver.cloud.gwc.bus")
public class GeoWebCacheRemoteEventsBroker {

    /**
     * Supplies the local service identifier to be used as "origin service id" when publishing
     * {@link RemoteGeoWebCacheEvent}s
     */
    private final @NonNull Supplier<String> originServiceId;

    /**
     * Function that returns whether a given remote application event is originated in the local
     * service instance ({@code true} or comes from a remote service ({@code false}).
     */
    private final @NonNull Function<RemoteApplicationEvent, Boolean> busServiceMatcher;

    /** The event publisher, usually the {@link ApplicationContext} itself */
    private final @NonNull Consumer<Object> eventPublisher;

    private final RemoteEventMapper mapper = Mappers.getMapper(RemoteEventMapper.class);

    @EventListener(GeoWebCacheEvent.class)
    public void publishRemoteEvent(GeoWebCacheEvent localEvent) throws CatalogException {
        if (isFromSelf(localEvent)) {
            String originService = originService();
            RemoteGeoWebCacheEvent remoteEvent = mapper.toRemote(localEvent, this, originService);
            publish(localEvent, remoteEvent);
        }
    }

    @EventListener(RemoteGeoWebCacheEvent.class)
    public void publishLocalEvent(RemoteGeoWebCacheEvent remoteEvent) {
        if (!isFromSelf(remoteEvent)) {
            GeoWebCacheEvent localEvent = mapper.toLocal(remoteEvent, this);
            publish(remoteEvent, localEvent);
        }
    }

    private @NonNull String originService() {
        return originServiceId.get();
    }

    private void publish(@NonNull Object origEvent, @NonNull Object mappedEvent) {
        log.debug("publishing {} from {}", mappedEvent, origEvent);
        this.eventPublisher.accept(mappedEvent);
    }

    private boolean isFromSelf(GeoWebCacheEvent localEvent) {
        return localEvent.getSource() != this;
    }

    private boolean isFromSelf(RemoteGeoWebCacheEvent remoteEvent) {
        return busServiceMatcher.apply(remoteEvent);
    }

    @Override
    public String toString() {
        return "%s(%s)".formatted(getClass().getSimpleName(), originService());
    }
}
