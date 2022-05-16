/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.service;

import lombok.NonNull;
import lombok.Value;

import org.gwc.tiling.model.CacheIdentifier;
import org.gwc.tiling.model.MetaTileIdentifier;
import org.gwc.tiling.model.TileIdentifier;

import java.time.Instant;
import java.util.stream.Stream;

/**
 * @since 1.0
 */
public @Value class MetaTileRequest {

    private @NonNull String jobId;
    private @NonNull MetaTileIdentifier metaTile;
    private @NonNull Instant requestTimestamp;

    public CacheIdentifier cache() {
        return metaTile.getCache();
    }

    public Stream<TileIdentifier> tiles() {
        return metaTile.asTiles();
    }
}
