/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.integration.local;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.gwc.tiling.model.GridSubsetInfo;
import org.gwc.tiling.model.TilePyramid;
import org.gwc.tiling.model.TileRange3D;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @since 1.0
 */
@RequiredArgsConstructor
public class GridSubsetInfoAdapter implements GridSubsetInfo {

    private final @NonNull GridSubset subset;

    private TilePyramid pyramid;

    @Override
    public String getName() {
        return subset.getName();
    }

    @Override
    public int getMinZoomLevel() {
        return subset.getZoomStart();
    }

    @Override
    public int getMaxZoomLevel() {
        return subset.getZoomStop();
    }

    @Override
    public Integer getMinCachedZoomLevel() {
        return subset.getMinCachedZoom();
    }

    @Override
    public Integer getMaxCachedZoomLevel() {
        return subset.getMaxCachedZoom();
    }

    @Override
    public TilePyramid createTilePyramid(
            int metaTilingFactorX, int metaTilingFactorY, BoundingBox bounds) {
        if (null == pyramid) pyramid = buildPyramid(metaTilingFactorX, metaTilingFactorY, bounds);
        return pyramid;
    }

    /**
     * @return
     */
    private TilePyramid buildPyramid(
            int metaTilingFactorX, int metaTilingFactorY, BoundingBox bounds) {
        long[][] coveredGridLevels =
                resolveCoverageGridLevels(metaTilingFactorX, metaTilingFactorY, bounds);
        SortedSet<TileRange3D> levelRanges =
                IntStream //
                        .rangeClosed(getMinZoomLevel(), getMaxZoomLevel()) //
                        .mapToObj(level -> coveredGridLevels[level]) //
                        .map(this::gridCoverageToTileRange) //
                        .collect(Collectors.toCollection(TreeSet::new));
        return new TilePyramid(levelRanges);
    }

    private TileRange3D gridCoverageToTileRange(long[] levelCoverage) {
        int z = (int) levelCoverage[4];
        long minx = levelCoverage[0];
        long miny = levelCoverage[1];
        long maxx = levelCoverage[2];
        long maxy = levelCoverage[3];
        return TileRange3D.of(z, minx, miny, maxx, maxy);
    }

    private long[][] resolveCoverageGridLevels(int metaW, int metaH, BoundingBox boundingBox) {
        final GridSubset subset = this.subset;
        long[][] coveredGridLevels =
                boundingBox == null
                        ? subset.getCoverages()
                        : subset.getCoverageIntersections(boundingBox);
        int[] metaTilingFactors = {metaW, metaH};
        coveredGridLevels = subset.expandToMetaFactors(coveredGridLevels, metaTilingFactors);
        return coveredGridLevels;
    }
}
