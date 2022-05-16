/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import org.geowebcache.grid.BoundingBox;

/**
 * @since 1.0
 */
public interface GridSubsetInfo {

    String getName();

    int getMinZoomLevel();

    int getMaxZoomLevel();

    Integer getMinCachedZoomLevel();

    Integer getMaxCachedZoomLevel();

    /**
     * @param metaTilingFactorX
     * @param metaTilingFactorY
     * @param bounds
     * @return
     */
    TilePyramid createTilePyramid(int metaTilingFactorX, int metaTilingFactorY, BoundingBox bounds);

    /**
     * @return
     */
    default int numLevels() {
        return 1 + (getMaxZoomLevel() - getMinZoomLevel());
    }
}
