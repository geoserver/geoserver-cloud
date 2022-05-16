/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @since 1.0
 * @see TilePyramidBuilder
 * @see #builder()
 */
@Value
@Builder
@Jacksonized
public class TilePyramid {

    private SortedSet<TileRange3D> ranges;

    public TilePyramid(@NonNull SortedSet<TileRange3D> ranges) {
        this.ranges = Collections.unmodifiableSortedSet(new TreeSet<>(ranges));
    }

    @JsonIgnore
    public boolean isEmpty() {
        return count().equals(BigInteger.ZERO);
    }

    /**
     * @return the total number of tiles in the pyramid
     */
    public BigInteger count() {
        return ranges.stream()
                .map(TileRange3D::count)
                .reduce(BigInteger::add)
                .orElse(BigInteger.ZERO);
    }

    public BigInteger countMetaTiles(int width, int height) {
        return ranges.stream()
                .map(r -> r.countMetaTiles(width, height))
                .reduce(BigInteger::add)
                .orElse(BigInteger.ZERO);
    }

    public int minZoomLevel() {
        return ranges.first().getZoomLevel();
    }

    public int maxZoomLevel() {
        return ranges.last().getZoomLevel();
    }

    public Optional<TileRange3D> range(int zoomLevel) {
        return ranges.stream().filter(r -> r.getZoomLevel() == zoomLevel).findFirst();
    }

    /**
     * @return a stream to traverse all the {@link TileIndex3D tile indices} in the pyramid
     * @see TileRange3D#asTiles()
     */
    public Stream<TileIndex3D> asTiles() {
        return ranges.stream().map(TileRange3D::asTiles).flatMap(Function.identity());
    }

    public Stream<TileRange3D> asMetaTiles(final int width, final int height) {
        return ranges.stream()
                .map(range -> range.asMetatiles(width, height))
                .flatMap(Function.identity());
    }

    public TilePyramid fromLevel(int minZLevel) {
        return subset(minZLevel, maxZoomLevel());
    }

    public TilePyramid toLevel(int maxZLevel) {
        return subset(minZoomLevel(), maxZLevel);
    }

    public TilePyramid subset(int minZLevel, int maxZLevel) {
        if (minZLevel < 0 || minZLevel > maxZLevel)
            throw new IllegalArgumentException("minZLevel must be > 0 and <= maxZLevel");

        TreeSet<TileRange3D> subrange =
                ranges.stream()
                        .filter(r -> r.getZoomLevel() >= minZLevel && r.getZoomLevel() <= maxZLevel)
                        .collect(Collectors.toCollection(TreeSet::new));

        return new TilePyramid(subrange);
    }
}
