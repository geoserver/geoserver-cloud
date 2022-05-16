/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.service;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geowebcache.filter.parameters.ParametersUtils;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.mime.MimeException;
import org.geowebcache.mime.MimeType;
import org.gwc.tiling.model.CacheIdentifier;
import org.gwc.tiling.model.CacheIdentifier.CacheIdentifierBuilder;
import org.gwc.tiling.model.CacheJobRequest;
import org.gwc.tiling.model.CacheJobRequest.Action;
import org.gwc.tiling.model.TileLayerInfo;
import org.gwc.tiling.model.TilePyramid;
import org.gwc.tiling.model.TilePyramidBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @since 1.0
 */
@RequiredArgsConstructor
public class CacheJobRequestBuilder {

    private final @NonNull Function<String, TileLayerInfo> tileLayerResolver;
    private final @NonNull Function<String, Set<String>> tileLayerParametersIdsResolver;

    private Action action = Action.SEED;
    private String layerName;
    private Set<String> formats = new HashSet<>();

    private String gridsetId;
    private BoundingBox bounds;

    private Map<String, String> parameters = Map.of();
    private String parametersId;

    private Integer minZooomLevel;
    private Integer maxZooomLevel;

    public CacheJobRequestBuilder action(@NonNull Action action) {
        this.action = action;
        return this;
    }

    public CacheJobRequestBuilder layer(@NonNull String layer) {
        this.layerName = layer;
        return this;
    }

    public CacheJobRequestBuilder gridsetId(@NonNull String gridsetId) {
        this.gridsetId = gridsetId;
        return this;
    }

    public CacheJobRequestBuilder format(@NonNull String format) {
        this.formats.add(format);
        return this;
    }

    public CacheJobRequestBuilder parameters(@NonNull Map<String, String> parameters) {
        this.parameters = Map.copyOf(parameters);
        this.parametersId = null;
        return this;
    }

    public CacheJobRequestBuilder parametersId(@NonNull String parametersId) {
        this.parametersId = parametersId;
        this.parameters = Map.of();
        return this;
    }

    public CacheJobRequestBuilder tilesFromBounds(@NonNull BoundingBox bounds) {
        this.bounds = bounds;
        return this;
    }

    public CacheJobRequestBuilder minZoomLevel(Integer minZoom) {
        this.minZooomLevel = minZoom;
        return this;
    }

    public CacheJobRequestBuilder maxZoomLevel(Integer maxZoom) {
        this.maxZooomLevel = maxZoom;
        return this;
    }

    public List<CacheJobRequest> build() {
        final Instant timestamp = Instant.now();
        final TileLayerInfo layer = resolveLayer();
        Stream<CacheIdentifier> cacheIds = buildCacheIds(layer);

        return cacheIds.map(
                        cacheId -> {
                            String gridsetId = cacheId.getGridsetId();
                            TilePyramid tiles = resolveTilePyramid(layer, gridsetId);
                            return new CacheJobRequest(action, cacheId, tiles, timestamp);
                        })
                .toList();
    }

    private TileLayerInfo resolveLayer() {
        TileLayerInfo layer = this.tileLayerResolver.apply(this.layerName);
        Objects.requireNonNull(layer, () -> "Layer '" + this.layerName + "' couldn't be resolved");
        return layer;
    }

    private Stream<CacheIdentifier> buildCacheIds(TileLayerInfo layer) {
        String layerName = layer.getName();
        Set<String> gridsetIds = resolveGridsetIds(layer);
        List<String> formats = resolveFormats(layer);
        List<String> parametersIds = resolveParameterIds(layer);

        // Cartesian product of gridsets,formats, and paramsIds, for the layer
        return Stream.of(CacheIdentifier.builder().layerName(layerName))
                .flatMap(builder -> gridsetIds.stream().map(builder::gridsetId)) //
                .flatMap(builder -> formats.stream().map(builder::format))
                .flatMap(
                        builder ->
                                parametersIds.stream()
                                        .map(Optional::ofNullable)
                                        .map(builder::parametersId))
                .map(CacheIdentifierBuilder::build);
    }

    private List<String> resolveFormats(TileLayerInfo layer) {
        Set<String> layerFormats = layer.getFormats();
        Set<String> requestedFormats = requestedFormats(layerFormats);

        verifyRequestedFormatsAreSupported(layer, requestedFormats, layerFormats);

        return layerFormats.stream().filter(requestedFormats::contains).toList();
    }

    protected void verifyRequestedFormatsAreSupported(
            TileLayerInfo layer, Set<String> requestedFormats, Set<String> layerFormats) {

        SetView<String> unsupported = Sets.difference(requestedFormats, layerFormats);
        if (!unsupported.isEmpty()) {
            String msg =
                    String.format(
                            "The following formats are not supported by layer %s: %s",
                            layer.getName(),
                            unsupported.stream().collect(Collectors.joining(", ")));
            throw new IllegalStateException(msg);
        }
    }

    private Set<String> requestedFormats(Set<String> layerFormats) {
        return formats.isEmpty()
                ? layerFormats
                : formatNames(formats.stream().map(this::validateFormat));
    }

    private Set<String> formatNames(Stream<MimeType> mimeTypes) {
        return mimeTypes.map(MimeType::getFormat).collect(Collectors.toCollection(TreeSet::new));
    }

    private MimeType validateFormat(String format) {
        try {
            return MimeType.createFromFormat(format);
        } catch (MimeException e) {
            throw new IllegalStateException(e);
        }
    }

    private Set<String> resolveGridsetIds(TileLayerInfo layer) {
        Set<String> layerGridSubsets = layer.gridSubsetNames();
        Set<String> requestedSubsets =
                this.gridsetId == null ? layerGridSubsets : Set.of(this.gridsetId);
        verifyRequestedGridsets(layerGridSubsets, requestedSubsets);
        return requestedSubsets;
    }

    private void verifyRequestedGridsets(
            Set<String> layerGridSubsets, Set<String> requestedSubsets) {

        SetView<String> missing = Sets.difference(requestedSubsets, layerGridSubsets);
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    String.format(
                            "Layer is not configured for the following GriSets: %s",
                            missing.stream().collect(Collectors.joining(", "))));
        }
    }

    private TilePyramid resolveTilePyramid(
            @NonNull TileLayerInfo layer, @NonNull String gridsetId) {
        BoundingBox bounds = this.bounds;
        return TilePyramidBuilder.builder()
                .layer(layer)
                .gridsetId(gridsetId)
                .bounds(bounds)
                .minZoomLevel(minZooomLevel)
                .maxZoomLevel(maxZooomLevel)
                .build();
    }

    private List<String> resolveParameterIds(TileLayerInfo layer) {
        List<String> parameterIds;
        if (!this.parameters.isEmpty()) {
            parameterIds = List.of(ParametersUtils.getId(this.parameters));
        } else if (parametersId != null) {
            parameterIds = List.of(parametersId);
        } else {
            parameterIds = new ArrayList<>();
            final String defaultParamsId = null;
            parameterIds.add(defaultParamsId);
            String layerName = layer.getName();
            resolveExistingParameterIds(layerName).forEach(parameterIds::add);
        }
        return parameterIds;
    }

    private Set<String> resolveExistingParameterIds(String layerName) {
        Set<String> paramIds = tileLayerParametersIdsResolver.apply(layerName);
        Objects.requireNonNull(
                paramIds, () -> "Parameter ids for layer '" + layerName + "' couldn't be resolved");
        return paramIds;
    }
}
