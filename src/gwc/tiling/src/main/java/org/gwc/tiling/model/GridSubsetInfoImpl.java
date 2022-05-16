/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import org.geowebcache.grid.BoundingBox;

/**
 * @since 1.0
 */
@Value
@Builder
@Jacksonized
public class GridSubsetInfoImpl implements GridSubsetInfo {

    private String name;
    private int minZoomLevel;
    private int maxZoomLevel;
    private Integer minCachedZoomLevel;
    private Integer maxCachedZoomLevel;

    @Override
    public TilePyramid createTilePyramid(
            int metaTilingFactorX, int metaTilingFactorY, BoundingBox bounds) {

        throw new UnsupportedOperationException(
                "Not yet implemented, need an immutable representation of Gridset");
    }
}
