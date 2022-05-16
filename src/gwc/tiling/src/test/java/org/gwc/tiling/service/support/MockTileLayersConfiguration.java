/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.service.support;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geowebcache.config.TileLayerConfiguration;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.gwc.tiling.model.TileLayerMockSupport;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @since 1.0
 */
@RequiredArgsConstructor
final class MockTileLayersConfiguration implements TileLayerConfiguration {

    private final @NonNull TileLayerMockSupport mockLayers;

    public @Override Collection<? extends TileLayer> getLayers() {
        return mockLayers.getTileLayers();
    }

    public @Override Set<String> getLayerNames() {
        return getLayers().stream().map(TileLayer::getName).collect(Collectors.toSet());
    }

    public @Override Optional<TileLayer> getLayer(String layerName) {
        return getLayers().stream()
                .filter(tl -> tl.getName().equals(layerName))
                .map(TileLayer.class::cast)
                .findFirst();
    }

    public @Override int getLayerCount() {
        return getLayers().size();
    }

    public @Override boolean containsLayer(String layerName) {
        return getLayerNames().contains(layerName);
    }

    public @Override void deinitialize() throws Exception {}

    public @Override void afterPropertiesSet() throws Exception {}

    public @Override String getLocation() {
        return null;
    }

    public @Override String getIdentifier() {
        return "mockTileLayers";
    }

    public @Override void setGridSetBroker(GridSetBroker broker) {
        throw new UnsupportedOperationException();
    }

    public @Override void renameLayer(String oldName, String newName)
            throws NoSuchElementException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    public @Override void removeLayer(String layerName)
            throws NoSuchElementException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    public @Override void modifyLayer(TileLayer tl) throws NoSuchElementException {
        throw new UnsupportedOperationException();
    }

    public @Override boolean canSave(TileLayer tl) {
        throw new UnsupportedOperationException();
    }

    public @Override void addLayer(TileLayer tl) throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }
}
