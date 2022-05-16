/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigInteger;
import java.util.stream.Stream;

/**
 * @since 1.0
 */
@Value
@Builder
@AllArgsConstructor
@Jacksonized
public class TileRange3D implements Comparable<TileRange3D> {

    private int zoomLevel;

    private @NonNull TileRange2D tiles;

    public TileRange3D(@NonNull TileRange2D tiles, int zoomLevel) {
        this.zoomLevel = zoomLevel;
        this.tiles = tiles;
    }

    public static TileRange3D of(int z, long minx, long miny, long maxx, long maxy) {
        return new TileRange3D(TileRange2D.of(minx, miny, maxx, maxy), z);
    }

    /**
     * @return the total number of tiles in the range
     */
    public BigInteger count() {
        return tiles.count();
    }

    public BigInteger countMetaTiles(int width, int height) {
        return tiles.countMetaTiles(width, height);
    }

    public long minx() {
        return tiles.minx();
    }

    public long miny() {
        return tiles.miny();
    }

    public long maxx() {
        return tiles.maxx();
    }

    public long maxy() {
        return tiles.maxy();
    }

    public long spanX() {
        return tiles.spanX();
    }

    public long spanY() {
        return tiles.spanY();
    }

    /**
     * @return a stream to traverse all the {@link TileIndex3D tile indices} in the range
     * @see TileRange2D#asTiles()
     */
    public Stream<TileIndex3D> asTiles() {
        return tiles.asTiles().map(this::toIndex3D);
    }

    public Stream<TileRange3D> asMetatiles(int width, int height) {
        return tiles.asMetaTiles(width, height).map(this::toRange3D);
    }

    @Override
    public int compareTo(TileRange3D o) {
        int c = Integer.compare(zoomLevel, o.getZoomLevel());
        if (c == 0) {
            c = tiles.compareTo(o.getTiles());
        }
        return c;
    }

    private TileIndex3D toIndex3D(TileIndex2D xyIndex) {
        return new TileIndex3D(xyIndex, zoomLevel);
    }

    private TileRange3D toRange3D(TileRange2D xyRange) {
        return new TileRange3D(xyRange, zoomLevel);
    }
}
