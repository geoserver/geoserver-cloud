/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.repository;

import java.util.Optional;
import java.util.function.Consumer;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geowebcache.config.TileLayerConfiguration;
import org.geowebcache.layer.TileLayer;
import org.springframework.context.event.EventListener;

/**
 * GeoWebCache {@link TileLayerConfiguration} decorator to publish {@link TileLayerEvent}s.
 *
 * <p>The decorated {@link TileLayerConfiguration} shall work against GeoServer's {@link Catalog}
 * and produce {@link GeoServerTileLayer}s instead of simple {@link TileLayer}.
 *
 * @since 1.7
 */
public class GeoServerTileLayerConfiguration extends ForwardingTileLayerConfiguration {

    /**
     * Event publisher, used to send events whenever a {@code TileLayer} is added, changed, deleted.
     */
    @NonNull
    private Consumer<TileLayerEvent> eventPublisher;

    /**
     * Consumer of incoming events.
     *
     * @see #setEventListener
     */
    @NonNull
    private Consumer<TileLayerEvent> eventConsumer = e -> {};

    public GeoServerTileLayerConfiguration(
            @NonNull TileLayerConfiguration subject, Consumer<TileLayerEvent> eventPublisher) {
        super(subject);
        this.eventPublisher = eventPublisher;
    }

    /**
     * Used for client code to set an action to perform when a {@code TileLayerEvent} is received
     * instead of published (for example, when received from another cluster node). Can be used for
     * example to forward the event to the delegate {@code TileLayerConfiguration} is it can't
     * listen to events itself.
     */
    public void setEventListener(@NonNull Consumer<TileLayerEvent> consumer) {
        this.eventConsumer = consumer;
    }

    /** Dispatch incoming events to the consumer set in {@link #setEventListener} */
    @EventListener(TileLayerEvent.class)
    void onTileLayerEvent(TileLayerEvent event) {
        eventConsumer.accept(event);
    }

    @Override
    public void addLayer(@NonNull TileLayer tl) throws IllegalArgumentException {
        super.addLayer(tl);
        created(tl);
    }

    private void created(@NonNull TileLayer tl) {
        eventPublisher.accept(TileLayerEvent.created(this, publishedInfoId(tl), tl.getName()));
    }

    @Override
    public void removeLayer(@NonNull String layerName) {
        Optional<String> id = super.getLayer(layerName).map(this::publishedInfoId);
        super.removeLayer(layerName);
        id.ifPresent(publishedId -> deleted(publishedId, layerName));
    }

    private void deleted(String layerId, String layerName) {
        eventPublisher.accept(TileLayerEvent.deleted(this, layerId, layerName));
    }

    @Override
    public void modifyLayer(@NonNull TileLayer tl) {
        super.modifyLayer(tl);
        modified(tl);
    }

    @Override
    public void renameLayer(@NonNull String oldName, @NonNull String newName) {
        super.renameLayer(oldName, newName);
        super.getLayer(newName).ifPresent(newl -> renamed(newl, oldName));
    }

    private void renamed(@NonNull TileLayer newl, @NonNull String oldName) {
        modified(newl, oldName);
    }

    private void modified(@NonNull TileLayer layer) {
        modified(layer, null);
    }

    private void modified(@NonNull TileLayer layer, String oldName) {
        String id = publishedInfoId(layer);
        String name = layer.getName();
        TileLayerEvent event = TileLayerEvent.modified(this, id, name, oldName);
        eventPublisher.accept(event);
    }

    private String publishedInfoId(TileLayer tl) {
        GeoServerTileLayer gstl = (GeoServerTileLayer) tl;
        return gstl.getId();
    }
}
