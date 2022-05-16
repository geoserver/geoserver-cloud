/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * @since 1.0
 */
class TileRange2DTest {

    /**
     * Test method for {@link
     * org.gwc.tiling.model.TileRange2D#TileRange2D(org.gwc.tiling.model.TileIndex2D,
     * org.gwc.tiling.model.TileIndex2D)}.
     */
    @Test
    void testTileRange2D() {
        assertThrows(IllegalArgumentException.class, () -> range(0, 0, -1, 0));

        assertThrows(IllegalArgumentException.class, () -> range(0, 0, 0, -1));
    }

    /** Test method for {@link org.gwc.tiling.model.TileRange2D#spanX()}. */
    @Test
    void testSpanX() {
        assertEquals(1, range(0, 0, 0, 0).spanX());
        assertEquals(10, range(0, 0, 9, 0).spanX());
    }

    /** Test method for {@link org.gwc.tiling.model.TileRange2D#spanY()}. */
    @Test
    void testSpanY() {
        assertEquals(1, range(0, 0, 0, 0).spanY());
        assertEquals(10, range(0, 0, 0, 9).spanY());
    }

    /** Test method for {@link org.gwc.tiling.model.TileRange2D#asTiles()}. */
    @Test
    void testAsTiles() {
        testAsTiles(range(-1, -1, -1, -1), TileIndex2D.of(-1, -1));
        testAsTiles(
                range(-1, -1, 1, -1),
                TileIndex2D.of(-1, -1),
                TileIndex2D.of(0, -1),
                TileIndex2D.of(1, -1));

        testAsTiles(
                range(-1, -1, -1, 1),
                TileIndex2D.of(-1, -1),
                TileIndex2D.of(-1, 0),
                TileIndex2D.of(-1, 1));

        testAsTiles(
                range(0, 0, 2, 2),
                TileIndex2D.of(0, 0),
                TileIndex2D.of(1, 0),
                TileIndex2D.of(2, 0),
                TileIndex2D.of(0, 1),
                TileIndex2D.of(1, 1),
                TileIndex2D.of(2, 1),
                TileIndex2D.of(0, 2),
                TileIndex2D.of(1, 2),
                TileIndex2D.of(2, 2));
    }

    private void testAsTiles(TileRange2D range, TileIndex2D... expected) {
        var actual = range.asTiles().toList();
        assertEquals(Arrays.asList(expected), actual);
    }

    /** Test method for {@link org.gwc.tiling.model.TileRange2D#split(int, int)}. */
    @Test
    void testAsMetaTilesSingleTileRange() {
        var single = range(0, 0, 0, 0);
        assertEquals(List.of(single), single.asMetaTiles(1, 1).toList());
        assertEquals(List.of(single), single.asMetaTiles(10, 1).toList());
        assertEquals(List.of(single), single.asMetaTiles(1, 10).toList());
    }

    /** Test method for {@link org.gwc.tiling.model.TileRange2D#split(int, int)}. */
    @Test
    void testAsMetaTilesMetaTilingFactorsBiggerThanRange() {
        var orig = range(0, 0, 5, 5);
        assertEquals(List.of(orig), orig.asMetaTiles(6, 6).toList());
        assertEquals(List.of(orig), orig.asMetaTiles(10, 10).toList());
    }

    /** Test method for {@link org.gwc.tiling.model.TileRange2D#split(int, int)}. */
    @Test
    void testAsMetaTilesVerticalRangeHorizonalMetaTilingFactors() {
        int splitWidth = 7;
        int splitHeight = 3;

        var verticalRange = range(0, 0, 0, 9); // 1x10
        var expected =
                List.of(
                        range(0, 0, 0, 2), //
                        range(0, 3, 0, 5), //
                        range(0, 6, 0, 8), //
                        range(0, 9, 0, 9));

        var actual = verticalRange.asMetaTiles(splitWidth, splitHeight).toList();
        assertEquals(expected, actual);
    }

    /** Test method for {@link org.gwc.tiling.model.TileRange2D#split(int, int)}. */
    @Test
    void testAsMetaTilesHorizontalRangeVerticalMetaTilingFactors() {
        int splitWidth = 3;
        int splitHeight = 11;

        var verticalRange = range(0, 0, 9, 0); // 10x1
        var expected =
                List.of(
                        range(0, 0, 2, 0), //
                        range(3, 0, 5, 0), //
                        range(6, 0, 8, 0), //
                        range(9, 0, 9, 0));

        var actual = verticalRange.asMetaTiles(splitWidth, splitHeight).toList();
        assertEquals(expected, actual);
    }

    /** Test method for {@link org.gwc.tiling.model.TileRange2D#split(int, int)}. */
    @Test
    void testAsMetaTiles() {
        final int splitWidth = 4;
        final int splitHeight = 4;

        var verticalRange = range(0, 0, 9, 9);
        assertEquals(10, verticalRange.spanX());
        assertEquals(10, verticalRange.spanY());

        var expected =
                List.of(
                        // first raw of 4x4, last col is 2x4
                        range(0, 0, 3, 3), //
                        range(4, 0, 7, 3), //
                        range(8, 0, 9, 3), //
                        // second raw of 4x4, last col is 2x4
                        range(0, 4, 3, 7), //
                        range(4, 4, 7, 7), //
                        range(8, 4, 9, 7), //
                        // third raw of 4x4, last col is 2x2
                        range(0, 8, 3, 9), //
                        range(4, 8, 7, 9), //
                        range(8, 8, 9, 9) //
                        );

        var actual = verticalRange.asMetaTiles(splitWidth, splitHeight).toList();
        assertEquals(expected, actual);
    }

    @Test
    void testAsMetaTiles_invalid_arguments() {

        var range = range(0, 0, 9, 9);
        assertEquals(10, range.spanX());
        assertEquals(10, range.spanY());

        IllegalArgumentException ex;
        ex = assertThrows(IllegalArgumentException.class, () -> range.asMetaTiles(-1, 1));
        assertThat(ex.getMessage()).contains("width");
        ex = assertThrows(IllegalArgumentException.class, () -> range.asMetaTiles(0, 1));
        assertThat(ex.getMessage()).contains("width");
        ex = assertThrows(IllegalArgumentException.class, () -> range.asMetaTiles(1, -1));
        assertThat(ex.getMessage()).contains("height");
        ex = assertThrows(IllegalArgumentException.class, () -> range.asMetaTiles(1, 0));
        assertThat(ex.getMessage()).contains("height");
    }

    @Test
    void test_count() {
        assertEquals(100, range(0, 0, 9, 9).count().longValue());
        assertEquals(10, range(0, 0, 9, 0).count().longValue());
        assertEquals(10, range(0, 0, 0, 9).count().longValue());

        var maxInt = BigInteger.valueOf(Integer.MAX_VALUE);
        assertEquals(
                maxInt.multiply(maxInt), range(1, 1, Integer.MAX_VALUE, Integer.MAX_VALUE).count());
    }

    private TileRange2D range(int minx, int miny, int maxx, int maxy) {
        return TileRange2D.of(minx, miny, maxx, maxy);
    }
}
