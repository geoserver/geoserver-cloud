/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.geowebcache.filter.parameters.ParametersUtils;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.mime.ApplicationMime;
import org.geowebcache.mime.ImageMime;
import org.geowebcache.mime.MimeType;
import org.gwc.tiling.model.CacheIdentifier;
import org.gwc.tiling.model.CacheJobRequest;
import org.gwc.tiling.model.GridSubsetInfo;
import org.gwc.tiling.model.TileLayerInfo;
import org.gwc.tiling.model.TileLayerMockSupport;
import org.gwc.tiling.model.TilePyramid;
import org.gwc.tiling.model.TilePyramidBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @since 1.0
 */
class CacheJobRequestBuilderTest {

    private CacheJobRequestBuilder builder;

    private Map<String, TileLayerInfo> layers;
    private Map<String, Set<String>> paramIds;

    private final TileLayerMockSupport support = new TileLayerMockSupport();

    private final GridSubsetInfo subset3857 = support.subset3857;
    private final GridSubsetInfo subset4326 = support.subset4326;

    private TileLayerInfo mockLayer(
            String name,
            Set<GridSubsetInfo> gridSubsets,
            List<MimeType> mimeTypes,
            List<String> parameterIds) {
        TileLayerInfo l = support.mockLayer(name, gridSubsets, mimeTypes, parameterIds);
        this.paramIds.put(name, new HashSet<>(parameterIds));
        this.layers.put(name, l);
        return l;
    }

    @BeforeEach
    void setUp() throws Exception {
        layers = new HashMap<>();
        paramIds = new HashMap<>();
        builder =
                new CacheJobRequestBuilder(
                        layers::get, layer -> paramIds.getOrDefault(layer, Set.of()));
    }

    public @Test void
            builds_cartesian_product_of_gridsubsets_mimetypes_and_parameterids_plus_default_param_id() {

        Set<GridSubsetInfo> gridSubsets = Set.of(subset3857, subset4326);
        List<MimeType> mimeTypes = List.of(ImageMime.png, ApplicationMime.geojson);
        List<String> parameterIds = List.of("pid-1", "pid-2");
        mockLayer("layer:1", gridSubsets, mimeTypes, parameterIds);

        Set<CacheIdentifier> expected;
        {
            List<Optional<String>> paramIds =
                    Stream.concat(
                                    Stream.of(Optional.<String>empty()),
                                    parameterIds.stream().map(Optional::of))
                            .toList();

            expected =
                    Stream.of(CacheIdentifier.builder().layerName("layer:1"))
                            .flatMap(
                                    builder ->
                                            gridSubsets.stream()
                                                    .map(GridSubsetInfo::getName)
                                                    .map(builder::gridsetId))
                            .flatMap(
                                    builder ->
                                            mimeTypes.stream()
                                                    .map(MimeType::getFormat)
                                                    .map(builder::format))
                            .flatMap(builder -> paramIds.stream().map(builder::parametersId))
                            .map(CacheIdentifier.CacheIdentifierBuilder::build)
                            .collect(Collectors.toSet());
        }
        assertEquals(12, expected.size());

        List<CacheJobRequest> requests = builder.layer("layer:1").build();
        assertEquals(12, requests.size());

        Set<CacheIdentifier> actual =
                requests.stream().map(CacheJobRequest::getCacheId).collect(Collectors.toSet());
        assertEquals(expected, actual);
    }

    public @Test void test_action_defaults_to_seed() {
        mockLayer("layer:1", Set.of(subset4326), List.of(ImageMime.png), List.of());

        List<CacheJobRequest> requests = builder.layer("layer:1").build();
        assertEquals(1, requests.size());
        assertEquals(CacheJobRequest.Action.SEED, requests.get(0).getAction());
    }

    public @Test void test_parameterId_overrides_parameters() {
        mockLayer("layer:1", Set.of(subset4326), List.of(ImageMime.png), List.of());

        List<CacheJobRequest> requests =
                builder.layer("layer:1") //
                        .parameters(Map.of("p1", "v1", "p2", "v2")) //
                        .parametersId("param-id")
                        .build();
        assertEquals(1, requests.size());
        assertEquals("param-id", requests.get(0).getCacheId().getParametersId().orElseThrow());
    }

    public @Test void test_parameters_overrides_parametersId() {
        mockLayer("layer:1", Set.of(subset4326), List.of(ImageMime.png), List.of());

        Map<String, String> parameters = Map.of("p1", "v1", "p2", "v2");
        List<CacheJobRequest> requests =
                builder.layer("layer:1") //
                        .parametersId("param-id")
                        .parameters(parameters) //
                        .build();
        assertEquals(1, requests.size());

        String expected = ParametersUtils.getId(parameters);
        assertNotNull(expected);

        String actual = requests.get(0).getCacheId().getParametersId().orElseThrow();
        assertEquals(expected, actual);
    }

    public @Test void test_invalid_gridsetId() {
        mockLayer("layer:1", Set.of(subset4326), List.of(ImageMime.png), List.of());

        builder =
                builder.layer("layer:1") //
                        .gridsetId(subset3857.getName());
        IllegalStateException thrown = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(
                "Layer is not configured for the following GriSets: GoogleMapsCompatible",
                thrown.getMessage());
    }

    public @Test void test_gridsetId() {
        mockLayer(
                "layer:1",
                Set.of(subset4326, subset3857),
                List.of(ImageMime.png, ImageMime.jpeg),
                List.of());

        List<CacheJobRequest> requests =
                builder.layer("layer:1") //
                        .gridsetId(subset3857.getName())
                        .build();

        assertEquals(2, requests.size());
        assertEquals(
                Set.of(subset3857.getName()),
                requests.stream()
                        .map(CacheJobRequest::getCacheId)
                        .map(CacheIdentifier::getGridsetId)
                        .collect(Collectors.toSet()));
    }

    public @Test void test_format_invalid() {
        mockLayer(
                "layer:1",
                Set.of(subset3857, subset4326),
                List.of(ImageMime.png, ImageMime.jpeg),
                List.of());

        builder =
                builder.layer("layer:1") //
                        .format("not_a_valid_format");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, builder::build);
        assertThat(thrown.getMessage()).contains("Unsupported format");
    }

    public @Test void test_format_layer_not_supported() {
        mockLayer(
                "layer:1",
                Set.of(subset3857, subset4326),
                List.of(ImageMime.png, ImageMime.jpeg),
                List.of());

        builder =
                builder.layer("layer:1") //
                        .format(ImageMime.png24.getFormat());

        IllegalStateException thrown = assertThrows(IllegalStateException.class, builder::build);
        assertEquals(
                "The following formats are not supported by layer layer:1: image/png24",
                thrown.getMessage());
    }

    public @Test void test_formats_single() {
        mockLayer(
                "layer:1",
                Set.of(subset3857, subset4326),
                List.of(ImageMime.png, ImageMime.jpeg),
                List.of());

        List<CacheJobRequest> requests =
                builder.layer("layer:1") //
                        .format(ImageMime.jpeg.getFormat()) //
                        .build();

        assertEquals(2, requests.size());
        assertEquals(
                Set.of(ImageMime.jpeg.getFormat()),
                requests.stream()
                        .map(CacheJobRequest::getCacheId)
                        .map(CacheIdentifier::getFormat)
                        .collect(Collectors.toSet()));
    }

    public @Test void test_formats_multiple() {
        mockLayer(
                "layer:1",
                Set.of(subset3857, subset4326),
                List.of(ImageMime.png, ImageMime.jpeg, ApplicationMime.mapboxVector),
                List.of());

        List<CacheJobRequest> requests =
                builder.layer("layer:1") //
                        .format(ImageMime.jpeg.getFormat()) //
                        .format(ApplicationMime.mapboxVector.getFormat()) //
                        .build();

        assertEquals(4, requests.size());
        assertEquals(
                Set.of(ImageMime.jpeg.getFormat(), ApplicationMime.mapboxVector.getFormat()),
                requests.stream()
                        .map(CacheJobRequest::getCacheId)
                        .map(CacheIdentifier::getFormat)
                        .collect(Collectors.toSet()));
    }

    public @Test void test_formats_all() {
        mockLayer(
                "layer:1",
                Set.of(subset3857, subset4326),
                List.of(ImageMime.png, ImageMime.jpeg, ApplicationMime.mapboxVector),
                List.of());

        List<CacheJobRequest> requests =
                builder.layer("layer:1") //
                        .build();

        assertEquals(6, requests.size());
        assertEquals(
                Set.of(
                        ImageMime.jpeg.getFormat(),
                        ApplicationMime.mapboxVector.getFormat(),
                        ImageMime.png.getFormat()),
                requests.stream()
                        .map(CacheJobRequest::getCacheId)
                        .map(CacheIdentifier::getFormat)
                        .collect(Collectors.toSet()));
    }

    public @Test void test_min_max_zoom_level() {
        GridSubsetInfo gridSubset = subset3857;
        mockLayer("layer:1", Set.of(gridSubset), List.of(ImageMime.png, ImageMime.jpeg), List.of());

        builder = builder.layer("layer:1");

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
            CacheJobRequestBuilder builder, GridSubsetInfo gridSubset, Integer min, Integer max) {

        final int subsetMin =
                Optional.ofNullable(gridSubset.getMinCachedZoomLevel())
                        .orElse(gridSubset.getMinZoomLevel());
        final int subsetMax =
                Optional.ofNullable(gridSubset.getMaxCachedZoomLevel())
                        .orElse(gridSubset.getMaxZoomLevel());

        int expectedMin = min == null ? subsetMin : min;
        int expectedMax = max == null ? subsetMax : max;

        List<CacheJobRequest> reqs = builder.minZoomLevel(min).maxZoomLevel(max).build();
        reqs.forEach(
                req -> {
                    TilePyramid tiles = req.getTiles();
                    assertEquals(expectedMin, tiles.minZoomLevel());
                    assertEquals(expectedMax, tiles.maxZoomLevel());
                });
    }

    public @Test void test_full_bounds() {
        TileLayerInfo layer =
                mockLayer(
                        "layer:1",
                        Set.of(subset4326, subset3857),
                        List.of(ImageMime.png),
                        List.of());

        final TilePyramid fullBundsPyramid4326 =
                TilePyramidBuilder.builder().layer(layer).gridsetId(subset4326.getName()).build();

        final TilePyramid fullBundsPyramid3857 =
                TilePyramidBuilder.builder().layer(layer).gridsetId(subset3857.getName()).build();

        builder = builder.layer("layer:1");

        List<CacheJobRequest> requests = builder.gridsetId(subset4326.getName()).build();
        assertEquals(1, requests.size());
        CacheJobRequest req = requests.get(0);
        TilePyramid tiles = req.getTiles();
        assertEquals(fullBundsPyramid4326, tiles);

        requests = builder.gridsetId(subset3857.getName()).build();
        assertEquals(1, requests.size());
        req = requests.get(0);
        tiles = req.getTiles();
        assertEquals(fullBundsPyramid3857, tiles);
    }

    public @Test void test_tilesFromBounds() {
        TileLayerInfo layer =
                mockLayer(
                        "layer:1",
                        Set.of(subset4326, subset3857),
                        List.of(ImageMime.png),
                        List.of());

        final String gridsetId = subset4326.getName();
        final BoundingBox leftHemisphere = new BoundingBox(-180, -90, 0, 90);

        final TilePyramid leftHemispherePyramid =
                TilePyramidBuilder.builder()
                        .layer(layer)
                        .gridsetId(gridsetId)
                        .bounds(leftHemisphere)
                        .build();

        List<CacheJobRequest> requests =
                builder.layer("layer:1") //
                        .gridsetId(subset4326.getName()) //
                        .tilesFromBounds(leftHemisphere) //
                        .build();

        assertEquals(1, requests.size());
        CacheJobRequest req = requests.get(0);
        TilePyramid tiles = req.getTiles();
        assertEquals(leftHemispherePyramid, tiles);
    }
}
