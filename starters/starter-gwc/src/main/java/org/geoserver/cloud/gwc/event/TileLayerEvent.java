/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.event;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.geoserver.gwc.layer.TileLayerCatalogListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

/**
 * Local {@link ApplicationContext} event issued to replace the tighly coupled {@link
 * TileLayerCatalogListener} by loosely coupled application events
 *
 * @since 1.0
 */
public abstract class TileLayerEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    public static enum Type {
        CREATE,
        MODIFY,
        DELETE
    }

    private @Getter @Setter Type eventType;
    private @Getter @Setter String layerId;

    public TileLayerEvent(Object source, @NonNull String layerId, @NonNull Type eventType) {
        super(source);
        this.eventType = eventType;
        this.layerId = layerId;
    }

    public @Override String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), getLayerId());
    }
}
