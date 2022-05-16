/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.gwc.tiling.model.CacheIdentifier;
import org.gwc.tiling.model.CacheJobRequest;
import org.gwc.tiling.model.MetaTileIdentifier;
import org.gwc.tiling.model.TilePyramid;
import org.gwc.tiling.model.TileRange3D;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @since 1.0
 */
@RequiredArgsConstructor
public class TileCacheManager {

    private final @NonNull Function<CacheIdentifier, Optional<TileLayerSeeder>> layerResolver;

    public Stream<MetaTileIdentifier> splitMetaTiles(@NonNull CacheJobRequest request) {

        final CacheIdentifier cache = request.getCacheId();
        Stream<TileRange3D> metaTileRanges = resolveMetaTiles(cache, request.getTiles());

        return metaTileRanges.map(range -> new MetaTileIdentifier(cache, range));
    }

    public void seed(@NonNull MetaTileRequest metatile) {
        CacheIdentifier cacheId = metatile.cache();
        TileLayerSeeder layer = resolveLayer(cacheId);
        layer.seed(metatile);
    }

    public void reseed(@NonNull MetaTileRequest metatile) {
        CacheIdentifier cacheId = metatile.cache();
        TileLayerSeeder layer = resolveLayer(cacheId);
        layer.reseed(metatile);
    }

    public void truncate(@NonNull MetaTileRequest metatile) {
        CacheIdentifier cacheId = metatile.cache();
        TileLayerSeeder layer = resolveLayer(cacheId);
        layer.truncate(metatile);
    }

    protected Stream<TileRange3D> resolveMetaTiles(CacheIdentifier cache, TilePyramid tiles) {

        final TileLayerSeeder layer = resolveLayer(cache);
        int width = layer.getMetaWidth();
        int height = layer.getMetaHeight();

        return tiles.asMetaTiles(width, height);
    }

    private TileLayerSeeder resolveLayer(CacheIdentifier cache) {
        return layerResolver.apply(cache).orElseThrow();
    }
}
