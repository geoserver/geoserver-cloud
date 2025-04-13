/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.bus;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.cloud.event.GeoServerEvent;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("serial")
public class RemoteGeoServerEvent extends RemoteApplicationEvent {

    @Getter
    @NonNull
    private GeoServerEvent event;

    /** Deserialization-time constructor, {@link #getSource()} will be {@code null} */
    @SuppressWarnings("java:S2637") // final fields initialized by deserialization
    protected RemoteGeoServerEvent() {
        // default constructor, needed for deserialization
    }

    /** Publish-time constructor, {@link #getSource()} won't be {@code null} */
    public RemoteGeoServerEvent(
            Object source,
            @NonNull GeoServerEvent event,
            @NonNull String originService,
            @NonNull Destination destination) {
        super(source, originService, destination);
        this.event = event;
    }

    @Override
    public String toString() {
        return "[%s id: '%s', originService: '%s', destinationService: '%s', payload: %s]"
                .formatted(
                        getClass().getSimpleName(), getId(), getOriginService(), getDestinationService(), getEvent());
    }

    public String toShortString() {
        return getEvent().toShortString();
    }
}
