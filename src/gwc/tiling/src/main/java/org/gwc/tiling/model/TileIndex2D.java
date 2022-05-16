/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.function.LongUnaryOperator;

/**
 * @since 1.0
 */
@Value
@Builder
@Jacksonized
public class TileIndex2D implements Comparable<TileIndex2D> {
    private long x;
    private long y;

    @Override
    public int compareTo(TileIndex2D o) {
        int c = Long.compare(x, o.getX());
        if (c == 0) {
            c = Long.compare(y, o.getY());
        }
        return c;
    }

    public TileIndex2D shiftX(long deltaX) {
        return shiftBy(deltaX, 0L);
    }

    public TileIndex2D shiftY(long deltaY) {
        return shiftBy(0L, deltaY);
    }

    public TileIndex2D shiftBy(long deltaX, long deltaY) {
        return TileIndex2D.of(x + deltaX, y + deltaY);
    }

    public TileIndex2D shiftBy(LongUnaryOperator xfunction, LongUnaryOperator yfunction) {
        return shiftBy(xfunction.applyAsLong(x), yfunction.applyAsLong(y));
    }

    public static TileIndex2D of(long x, long y) {
        return new TileIndex2D(x, y);
    }
}
