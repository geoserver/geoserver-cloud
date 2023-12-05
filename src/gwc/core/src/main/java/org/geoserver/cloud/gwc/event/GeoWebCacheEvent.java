/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.event;

import lombok.Getter;
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
public abstract class GeoWebCacheEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    public enum Type {
        CREATED,
        MODIFIED,
        DELETED
    }

    private @Getter @Setter Type eventType;
    private @Getter @Setter String id;

    protected GeoWebCacheEvent(Object source) {
        this(source, null);
    }

    protected GeoWebCacheEvent(Object source, Type eventType) {
        super(source);
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[%s '%s' id: %s timestamp: %s]",
                getClass().getSimpleName(), getEventType(), getObjectId(), getId(), getTimestamp());
    }

    protected abstract String getObjectId();
}
