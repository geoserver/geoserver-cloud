/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.integration.local;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;
import org.gwc.tiling.model.GridSubsetInfo;
import org.gwc.tiling.model.TileLayerInfo;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @since 1.0
 */
@RequiredArgsConstructor
public class TileLayerInfoAdapter implements TileLayerInfo {

    private final @NonNull TileLayer layer;

    @Override
    public String getName() {
        return layer.getName();
    }

    @Override
    public Set<String> getFormats() {
        return layer.getMimeTypes().stream().map(MimeType::getFormat).collect(Collectors.toSet());
    }

    @Override
    public List<GridSubsetInfo> getGridSubsets() {
        return layer.getGridSubsets().stream()
                .map(layer::getGridSubset)
                .map(GridSubsetInfoAdapter::new)
                .map(GridSubsetInfo.class::cast)
                .toList();
    }

    @Override
    public int getMetaTilingWidth() {
        return layer.getMetaTilingFactors()[0];
    }

    @Override
    public int getMetaTilingHeight() {
        return layer.getMetaTilingFactors()[1];
    }

    @Override
    public Set<String> gridSubsetNames() {
        return layer.getGridSubsets();
    }

    @Override
    public Optional<GridSubsetInfo> gridSubset(@NonNull String gridsetId) {
        return Optional.ofNullable(layer.getGridSubset(gridsetId)).map(GridSubsetInfoAdapter::new);
    }
}
