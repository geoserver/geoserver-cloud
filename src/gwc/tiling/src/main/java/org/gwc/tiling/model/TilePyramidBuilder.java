/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.geowebcache.grid.BoundingBox;

import java.util.Objects;
import java.util.Optional;

/**
 * @since 1.0
 */
@Accessors(chain = true, fluent = true)
public class TilePyramidBuilder {

    private @Setter @Getter TileLayerInfo layer;
    private @Setter @Getter String gridsetId;
    private @Setter @Getter BoundingBox bounds;
    private @Setter @Getter Integer minZoomLevel;
    private @Setter @Getter Integer maxZoomLevel;

    public static TilePyramidBuilder builder() {
        return new TilePyramidBuilder();
    }

    public TilePyramid build() {
        Objects.requireNonNull(layer, "layer can't be null");
        Objects.requireNonNull(gridsetId, "gridsetId can't be null");

        final @NonNull GridSubsetInfo gridSubset = layer.gridSubset(gridsetId).orElseThrow();
        final int minLevel = resolveMinLevel(gridSubset);
        final int maxLevel = resolveMaxLevel(gridSubset);
        validateZoomLevels(minLevel, maxLevel);

        TilePyramid gridsetPyramid =
                gridSubset.createTilePyramid(
                        layer.getMetaTilingWidth(), layer.getMetaTilingHeight(), bounds);
        return gridsetPyramid.subset(minLevel, maxLevel);
    }

    protected void validateZoomLevels(final int minZoomLevel, final int maxZoomLevel) {
        if (minZoomLevel > maxZoomLevel) {
            throw new IllegalArgumentException(
                    String.format(
                            "Min zoom level (%d) can't be greater than max zoom level (%d)",
                            minZoomLevel, maxZoomLevel));
        }
    }

    private int resolveMinLevel(@NonNull GridSubsetInfo gridSubset) {
        final int subsetMin = subsetMinLevel(gridSubset);
        if (null == minZoomLevel) return subsetMin;

        if (minZoomLevel < subsetMin)
            throw new IllegalArgumentException(
                    String.format(
                            "Min zoom level must be >= than %d for grid subset %s. Got %d",
                            subsetMin, gridSubset.getName(), minZoomLevel));

        return minZoomLevel;
    }

    private int resolveMaxLevel(@NonNull GridSubsetInfo gridSubset) {
        final int subsetMax = subsetMaxLevel(gridSubset);
        if (null == maxZoomLevel) return subsetMax;
        if (maxZoomLevel > subsetMax)
            throw new IllegalArgumentException(
                    String.format(
                            "Max zoom level must be <= than %d for grid subset %s. Got %d",
                            subsetMax, gridSubset.getName(), maxZoomLevel));
        return maxZoomLevel;
    }

    private int subsetMinLevel(GridSubsetInfo gridSubset) {
        return Optional.ofNullable(gridSubset.getMinCachedZoomLevel()).orElse(0);
    }

    private int subsetMaxLevel(GridSubsetInfo gridSubset) {
        return Optional.ofNullable(gridSubset.getMaxCachedZoomLevel())
                .orElse(gridSubset.getMaxZoomLevel());
    }
}
