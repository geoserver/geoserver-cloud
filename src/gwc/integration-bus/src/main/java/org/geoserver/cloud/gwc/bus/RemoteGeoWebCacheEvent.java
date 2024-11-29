/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.bus;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

/**
 * @since 1.0
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class RemoteGeoWebCacheEvent extends RemoteApplicationEvent {

    private static final long serialVersionUID = 1L;

    public enum Type {
        CREATED,
        MODIFIED,
        DELETED
    }

    private static final Destination ALL = DEFAULT_DESTINATION_FACTORY.getDestination(null);

    private @Getter @Setter Type eventType;

    protected RemoteGeoWebCacheEvent(Object source, @NonNull String originService) {
        super(source, originService, ALL);
    }

    protected RemoteGeoWebCacheEvent(Object source, @NonNull String originService, @NonNull Type eventType) {
        super(source, originService, ALL);
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return "%s[%s '%s' id: %s origin: %s destination: %s timestamp: %s]"
                .formatted(
                        getClass().getSimpleName(),
                        getEventType(),
                        getObjectId(),
                        getId(),
                        getOriginService(),
                        getDestinationService(),
                        getTimestamp());
    }

    protected abstract String getObjectId();
}
