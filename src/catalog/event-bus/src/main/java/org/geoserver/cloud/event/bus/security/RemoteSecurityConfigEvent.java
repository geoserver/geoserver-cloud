/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.bus.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import org.geoserver.cloud.event.security.SecurityConfigChanged;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

@EqualsAndHashCode(callSuper = true)
public class RemoteSecurityConfigEvent extends RemoteApplicationEvent {
    private static final Destination ALL = DEFAULT_DESTINATION_FACTORY.getDestination(null);

    private static final long serialVersionUID = 1L;

    private @Getter @NonNull SecurityConfigChanged event;

    /** Deserialization-time constructor, {@link #getSource()} will be {@code null} */
    protected RemoteSecurityConfigEvent() {
        // default constructor, needed for deserialization
    }

    /** Publish-time constructor, {@link #getSource()} won't be {@code null} */
    public RemoteSecurityConfigEvent(
            @NonNull SecurityConfigChanged orig, @NonNull Object source, @NonNull String origin) {

        super(source, origin, ALL);
        this.event = orig;
    }

    @Override
    public String toString() {
        return String.format(
                "[%s id: '%s', originService: '%sd', destinationService: '%s', payload: %s]",
                getClass().getSimpleName(),
                getId(),
                getOriginService(),
                getDestinationService(),
                getEvent());
    }
}
