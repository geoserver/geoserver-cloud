/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.event;

import com.google.common.annotations.VisibleForTesting;
import javax.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.gwc.layer.TileLayerCatalogListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Adapts the listener pattern used by {@link TileLayerCatalog#addListener} and {@link
 * TileLayerCatalogListener} used to notify gwc tile layer events, as regular spring {@link
 * ApplicationEvent application events}, and publishes them to the local {@link ApplicationContext},
 * so other components interested in these kind of events don't need to register themselves as
 * {@link TileLayerCatalogListener}s.
 *
 * @see TileLayerAddedEvent
 * @see TileLayerRemovedEvent
 * @see TileLayerModifiedEvent
 * @see TileLayerRenamedEvent
 */
public class TileLayerEventPublisher {

    @Setter(value = AccessLevel.PACKAGE)
    private @Autowired ApplicationEventPublisher localContextPublisher;

    @Setter(value = AccessLevel.PACKAGE)
    private @Autowired @Qualifier("GeoSeverTileLayerCatalog") TileLayerCatalog tileLayerCatalog;

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
                return new TileLayerAddedEvent(this, layerId);
            case DELETE:
                return new TileLayerRemovedEvent(this, layerId);
            case MODIFY:
                return new TileLayerModifiedEvent(this, layerId);
            default:
                throw new IllegalArgumentException("type: " + type);
        }
    }

    @RequiredArgsConstructor
    @VisibleForTesting
    static class LocalTileEventPublisher implements TileLayerCatalogListener {
        private final TileLayerEventPublisher publisher;

        public @Override void onEvent(String layerId, TileLayerCatalogListener.Type type) {
            TileLayerEvent event = publisher.toEvent(layerId, type);
            publisher.publish(event);
        }
    }
}
