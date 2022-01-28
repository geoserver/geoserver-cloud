/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.bus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.geoserver.gwc.layer.TileLayerCatalogListener;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.ApplicationContext;

/**
 * Local {@link ApplicationContext} event issued to replace the tighly coupled {@link
 * TileLayerCatalogListener} by loosely coupled application events
 *
 * @since 1.0
 */
@NoArgsConstructor
public abstract class RemoteTileLayerEvent extends RemoteApplicationEvent {

    private static final long serialVersionUID = 1L;

    public static enum Type {
        CREATED,
        MODIFIED,
        DELETED
    }

    private static final Destination ALL = DEFAULT_DESTINATION_FACTORY.getDestination(null);

    private @Getter @Setter Type eventType;
    private @Getter @Setter String layerId;

    public RemoteTileLayerEvent(
            Object source,
            @NonNull String originService,
            @NonNull String layerId,
            @NonNull Type eventType) {
        super(source, originService, ALL);
        this.eventType = eventType;
        this.layerId = layerId;
    }

    public @Override String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), getLayerId());
    }
}
