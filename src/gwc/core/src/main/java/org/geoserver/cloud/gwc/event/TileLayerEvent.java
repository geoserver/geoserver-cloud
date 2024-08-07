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
import org.springframework.lang.Nullable;

/**
 * Local {@link ApplicationContext} event issued to replace the tightly coupled {@link
 * TileLayerCatalogListener} by loosely coupled application events
 *
 * @since 1.0
 */
public class TileLayerEvent extends GeoWebCacheEvent {

    private static final long serialVersionUID = 1L;

    private @NonNull @Getter @Setter String publishedId;
    private @NonNull @Getter @Setter String name;
    private @Getter @Setter String oldName;

    @SuppressWarnings("java:S2637")
    public TileLayerEvent(Object source) {
        super(source);
    }

    public TileLayerEvent(
            Object source,
            @NonNull Type eventType,
            @NonNull String layerId,
            @NonNull String layerName) {
        super(source, eventType);
        this.publishedId = layerId;
        this.name = layerName;
    }

    public static TileLayerEvent ofId(
            @NonNull Object source, @NonNull Type eventType, @NonNull String layerId) {
        return new TileLayerEvent(source, eventType, layerId, layerId);
    }

    public static TileLayerEvent created(
            @NonNull Object source, @NonNull String publishedId, @NonNull String layerName) {
        return valueOf(source, Type.CREATED, publishedId, layerName, null);
    }

    public static TileLayerEvent deleted(
            @NonNull Object source, @NonNull String publishedId, @NonNull String layerName) {
        return valueOf(source, Type.DELETED, publishedId, layerName, null);
    }

    public static TileLayerEvent modified(
            @NonNull Object source,
            @NonNull String publishedId,
            @NonNull String layerName,
            @Nullable String oldName) {
        return valueOf(source, Type.MODIFIED, publishedId, layerName, oldName);
    }

    private static TileLayerEvent valueOf(
            @NonNull Object source,
            @NonNull Type eventType,
            @NonNull String publishedId,
            @NonNull String layerName,
            String oldName) {
        TileLayerEvent event = new TileLayerEvent(source, eventType, publishedId, layerName);
        event.setOldName(oldName);
        return event;
    }

    @Override
    public String toString() {
        return "%s[%s id: %s, name: %s]"
                .formatted(getClass().getSimpleName(), getEventType(), getPublishedId(), getName());
    }

    protected @Override String getObjectId() {
        return publishedId;
    }
}
