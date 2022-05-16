/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * @since 1.0
 */
@Value
@Builder
@Jacksonized
public class TileRange2D implements Comparable<TileRange2D> {

    private @NonNull TileIndex2D lowerLeft;

    private @NonNull TileIndex2D upperRight;

    public TileRange2D(@NonNull TileIndex2D lowerLeft, @NonNull TileIndex2D upperRight) {
        verify(lowerLeft, upperRight);
        this.lowerLeft = lowerLeft;
        this.upperRight = upperRight;
    }

    public static TileRange2D of(long minx, long miny, long maxx, long maxy) {
        return new TileRange2D(TileIndex2D.of(minx, miny), TileIndex2D.of(maxx, maxy));
    }

    /**
     * @return the total number of tiles in the range
     */
    public BigInteger count() {
        long spanX = spanX();
        long spanY = spanY();
        return BigInteger.valueOf(spanX).multiply(BigInteger.valueOf(spanY));
    }

    public long minx() {
        return lowerLeft.getX();
    }

    public long miny() {
        return lowerLeft.getY();
    }

    public long maxx() {
        return upperRight.getX();
    }

    public long maxy() {
        return upperRight.getY();
    }

    public long spanX() {
        return 1 + (maxx() - minx());
    }

    public long spanY() {
        return 1 + (maxy() - miny());
    }

    /**
     * @return a stream to traverse all the {@link TileIndex2D tile indices} in the range
     */
    public Stream<TileIndex2D> asTiles() {
        return rangeY().mapToObj(this::toXStream).flatMap(Function.identity());
    }

    public Stream<TileRange2D> asMetaTiles(int width, int height) {
        checkArgument(width > 0, "width must be > 0");
        checkArgument(height > 0, "height must be > 0");

        UnaryOperator<TileRange2D> computeNext = this.nextSubrange(width, height);
        final TileRange2D first = computeNext.apply(this);
        return Stream.iterate(first, computeNext).takeWhile(Objects::nonNull);
    }

    public BigInteger countMetaTiles(int width, int height) {
        checkArgument(width > 0, "width must be > 0");
        checkArgument(height > 0, "height must be > 0");

        long metaX = countMetatiles(spanX(), width);
        long metaY = countMetatiles(spanY(), height);
        return BigInteger.valueOf(metaX).multiply(BigInteger.valueOf(metaY));
    }

    private long countMetatiles(long ntiles, int metaSize) {
        long rem = ntiles % metaSize;
        long metas = ntiles / metaSize;
        return (rem > 0 ? 1 : 0) + metas;
    }

    private UnaryOperator<TileRange2D> nextSubrange(int width, int height) {
        return prev -> {
            long minx, maxx, miny, maxy;
            if (null == prev) return null;
            if (TileRange2D.this == prev) {
                minx = this.minx();
                miny = this.miny();
            } else {
                minx = prev.maxx() + 1;
                if (minx > this.maxx()) {
                    minx = this.minx();
                    miny = prev.maxy() + 1;
                } else {
                    miny = prev.miny();
                }
            }
            if (miny > this.maxy()) {
                return null;
            }
            maxx = Math.min(this.maxx(), minx + width - 1);
            maxy = Math.min(this.maxy(), miny + height - 1);

            return TileRange2D.of(minx, miny, maxx, maxy);
        };
    }

    private void checkArgument(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    private Stream<TileIndex2D> toXStream(long y) {
        return rangeX().mapToObj(x -> TileIndex2D.of(x, y));
    }

    private LongStream rangeX() {
        return LongStream.rangeClosed(lowerLeft.getX(), upperRight.getX());
    }

    private LongStream rangeY() {
        return LongStream.rangeClosed(lowerLeft.getY(), upperRight.getY());
    }

    @Override
    public int compareTo(TileRange2D o) {
        int c = lowerLeft.compareTo(o.getLowerLeft());
        if (c == 0) c = upperRight.compareTo(o.getUpperRight());
        return c;
    }

    protected void verify(TileIndex2D lowerLeft, TileIndex2D upperRight) {
        if (upperRight.getX() < lowerLeft.getX())
            throw new IllegalArgumentException("upperRight.x < lowerLeft.x");
        if (upperRight.getY() < lowerLeft.getY())
            throw new IllegalArgumentException("upperRight.y < lowerLeft.y");
    }

    public @Override String toString() {
        return String.format(
                "TileRange2D[x: %,d - %,d, y: %,d - %,d]", minx(), maxx(), miny(), maxy());
    }
}
