/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import lombok.NonNull;

import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.mime.ImageMime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @since 1.0
 */
class TilePyramidTest {

    private final TileLayerMockSupport support = new TileLayerMockSupport();

    @BeforeEach
    void setUp() throws Exception {}

    TilePyramid full3857() {
        return full(support.subset3857);
    }

    TilePyramid full4326() {
        return full(support.subset4326);
    }

    TilePyramid full(GridSubsetInfo subset) {
        return builder(subset).build();
    }

    TilePyramidBuilder builder(GridSubsetInfo subset) {
        TileLayerInfo layer = support.mockLayer("layer", subset, ImageMime.png24);
        String gridsetId = subset.getName();
        return TilePyramidBuilder.builder().layer(layer).gridsetId(gridsetId);
    }

    @Test
    void testRangesOrder() {
        GridSubsetInfo subset = support.subset3857;
        TilePyramid pyramid = full(subset);

        final int numLevels = subset.numLevels();
        final SortedSet<TileRange3D> ranges = pyramid.getRanges();

        assertEquals(numLevels, pyramid.getRanges().size());
        assertEquals(subset.getMinZoomLevel(), pyramid.minZoomLevel());
        assertEquals(subset.getMaxZoomLevel(), pyramid.maxZoomLevel());

        List<Integer> expected = IntStream.range(0, numLevels).mapToObj(Integer::valueOf).toList();
        List<Integer> actual = ranges.stream().map(TileRange3D::getZoomLevel).toList();
        assertEquals(expected, actual);
    }

    @Test
    void test_min_max_zoom_level() {
        GridSubsetInfo gridSubset = support.subset3857;
        TilePyramidBuilder builder = builder(gridSubset);

        Class<IllegalArgumentException> expected = IllegalArgumentException.class;
        final int min =
                Optional.ofNullable(gridSubset.getMinCachedZoomLevel())
                        .orElse(gridSubset.getMinZoomLevel());
        final int max =
                Optional.ofNullable(gridSubset.getMaxCachedZoomLevel())
                        .orElse(gridSubset.getMaxZoomLevel());

        assertThrows(expected, builder.minZoomLevel(min - 1)::build);
        assertThrows(expected, builder.minZoomLevel(max + 1)::build);

        assertThrows(expected, builder.maxZoomLevel(min - 1)::build);
        assertThrows(expected, builder.maxZoomLevel(max + 1)::build);

        assertThrows(expected, builder.minZoomLevel(max).maxZoomLevel(min)::build);

        assertZoomLevels(builder, gridSubset, null, max);
        assertZoomLevels(builder, gridSubset, null, max - 1);
        assertZoomLevels(builder, gridSubset, min, null);
        assertZoomLevels(builder, gridSubset, min + 1, null);
        assertZoomLevels(builder, gridSubset, min + 1, max - 1);
        assertZoomLevels(builder, gridSubset, min + 1, min + 1);
    }

    private void assertZoomLevels(
            TilePyramidBuilder builder, GridSubsetInfo gridSubset, Integer min, Integer max) {

        final int subsetMin =
                Optional.ofNullable(gridSubset.getMinCachedZoomLevel())
                        .orElse(gridSubset.getMinZoomLevel());
        final int subsetMax =
                Optional.ofNullable(gridSubset.getMaxCachedZoomLevel())
                        .orElse(gridSubset.getMaxZoomLevel());

        int expectedMin = min == null ? subsetMin : min;
        int expectedMax = max == null ? subsetMax : max;

        TilePyramid tiles = builder.minZoomLevel(min).maxZoomLevel(max).build();
        assertEquals(expectedMin, tiles.minZoomLevel());
        assertEquals(expectedMax, tiles.maxZoomLevel());
    }

    @Test
    void testFullBounds_3857() {
        GridSet gridset = support.worldEpsg3857;
        GridSubset fullSubset = support.fullSubset(gridset);
        testFullBounds(support.subset3857, fullSubset);
    }

    @Test
    void testFullBounds_4326() {
        GridSet gridset = support.worldEpsg4326;
        GridSubset fullSubset = support.fullSubset(gridset);
        testFullBounds(support.subset4326, fullSubset);
    }

    void testFullBounds(@NonNull GridSubsetInfo subset, @NonNull GridSubset orig) {
        TilePyramidBuilder builder = builder(subset);
        TileLayerInfo layer = builder.layer();
        TilePyramid pyramid = builder.build();
        assertEquals(pyramid, builder(subset).bounds(null).build());

        long[][] coverages = orig.getCoverages();
        int[] metaTilingFactors = {layer.getMetaTilingWidth(), layer.getMetaTilingHeight()};
        // {z}{minx,miny,maxx,maxy,z}
        long[][] coveredGridLevels = orig.expandToMetaFactors(coverages, metaTilingFactors);

        for (int z = orig.getZoomStart(); z <= orig.getZoomStop(); z++) {
            long[] coverage = coveredGridLevels[z];

            TileRange3D range = pyramid.range(z).orElseThrow();
            assertEquals(z, range.getZoomLevel());
            TileIndex2D lowerLeft = range.getTiles().getLowerLeft();
            TileIndex2D upperRight = range.getTiles().getUpperRight();
            assertEquals(coverage[0], lowerLeft.getX(), () -> "at z level " + coverage[4]);
            assertEquals(coverage[1], lowerLeft.getY(), () -> "at z level " + coverage[4]);
            assertEquals(coverage[2], upperRight.getX(), () -> "at z level " + coverage[4]);
            assertEquals(coverage[3], upperRight.getY(), () -> "at z level " + coverage[4]);
        }
    }

    @Test
    void testAsMetaTiles() {
        final TilePyramid pyramid = full4326().toLevel(11);
        testAsMetaTiles(pyramid, 1, 1);
        testAsMetaTiles(pyramid, 2, 2);
        testAsMetaTiles(pyramid, 1, 3);
        testAsMetaTiles(pyramid, 3, 1);
        testAsMetaTiles(pyramid, 8, 8);
        testAsMetaTiles(pyramid, 21, 32);

        IllegalArgumentException err;
        err = assertThrows(IllegalArgumentException.class, () -> testAsMetaTiles(pyramid, -1, 1));
        assertThat(err.getMessage()).contains("width must be > 0");

        err = assertThrows(IllegalArgumentException.class, () -> testAsMetaTiles(pyramid, 1, -1));
        assertThat(err.getMessage()).contains("height must be > 0");
    }

    protected void testAsMetaTiles(final TilePyramid pyramid, int metax, int metay) {
        long metaTileCount = pyramid.asMetaTiles(metax, metay).count();
        long fastCount = pyramid.countMetaTiles(metax, metay).longValue();
        assertEquals(metaTileCount, fastCount);

        Stream<TileRange3D> asMetaTiles = pyramid.asMetaTiles(metax, metay);

        BigInteger tilesFromMetaTiles =
                asMetaTiles.map(TileRange3D::count).reduce(BigInteger::add).orElseThrow();

        assertEquals(pyramid.count(), tilesFromMetaTiles);
    }

    @Test
    void testAsTiles() {
        final TilePyramid pyramid = full4326().toLevel(12);
        Stream<TileIndex3D> tiles = pyramid.asTiles();
        assertEquals(pyramid.count().longValue(), tiles.count());
    }

    @Test
    @Disabled
    void testTraverseFullMetaTiles() {
        // e.g.: Count: 1,832,603,271, time: 36s, throughput: 50,905,646/s
        final TilePyramid pyramid = full4326().toLevel(18);
        long start = System.currentTimeMillis();
        long count = pyramid.asMetaTiles(10, 10).count();
        long ts = (System.currentTimeMillis() - start) / 1000;
        long thrpt = count / ts;
        System.err.printf("Count: %,d, time: %,ds, throughput: %,d/s", count, ts, thrpt);
    }
}
