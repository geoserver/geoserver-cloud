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

import java.util.stream.Stream;

/**
 * @since 1.0
 */
@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class MetaTileIdentifier {

    private @NonNull CacheIdentifier cache;
    private @NonNull TileRange3D tiles;

    public Stream<TileIdentifier> asTiles() {
        return tiles.asTiles().map(index -> new TileIdentifier(cache, index));
    }
}
