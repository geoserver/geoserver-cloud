/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.event;

import com.google.common.annotations.VisibleForTesting;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.gwc.layer.TileLayerCatalogListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import javax.annotation.PostConstruct;

/**
 * Adapts the listener pattern used by {@link TileLayerCatalog#addListener} and {@link
 * TileLayerCatalogListener} used to notify gwc tile layer events, as regular spring {@link
 * ApplicationEvent application events}, and publishes them to the local {@link ApplicationContext},
 * so other components interested in these kind of events don't need to register themselves as
 * {@link TileLayerCatalogListener}s.
 *
 * @see TileLayerEvent
 */
@RequiredArgsConstructor
public class TileLayerEventPublisher {

    private final @NonNull ApplicationEventPublisher localContextPublisher;
    private final @NonNull TileLayerCatalog tileLayerCatalog;

    private LocalTileEventPublisher tileLayerListener;

    public @PostConstruct void initialize() {
        tileLayerListener = new LocalTileEventPublisher(this);
        tileLayerCatalog.addListener(tileLayerListener);
    }

    public void publish(TileLayerEvent event) {
        localContextPublisher.publishEvent(event);
    }

    TileLayerEvent toEvent(@NonNull String layerId, @NonNull TileLayerCatalogListener.Type type) {
        switch (type) {
            case CREATE:
                return new TileLayerEvent(this, GeoWebCacheEvent.Type.CREATED, layerId);
            case DELETE:
                return new TileLayerEvent(this, GeoWebCacheEvent.Type.DELETED, layerId);
            case MODIFY:
                return new TileLayerEvent(this, GeoWebCacheEvent.Type.MODIFIED, layerId);
            default:
                throw new IllegalArgumentException("type: " + type);
        }
    }

    @RequiredArgsConstructor
    @VisibleForTesting
    static class LocalTileEventPublisher implements TileLayerCatalogListener {
        private final TileLayerEventPublisher publisher;

        @Override
        public void onEvent(String layerId, TileLayerCatalogListener.Type type) {
            TileLayerEvent event = publisher.toEvent(layerId, type);
            publisher.publish(event);
        }
    }
}
