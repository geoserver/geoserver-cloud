/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

/**
 * @since 1.0
 */
@Value
@With
@Builder
@Jacksonized
public class CacheJobStatistics {

    private long tilesCreated;
    private long tilesSkipped;
    private long tilesFailed;

    public static CacheJobStatistics newInstance() {
        return new CacheJobStatistics(0, 0, 0);
    }

    public @NonNull CacheJobStatistics merge(@NonNull CacheJobStatistics other) {
        return this.withTilesCreated(tilesCreated + other.getTilesCreated())
                .withTilesFailed(tilesFailed + other.getTilesFailed())
                .withTilesSkipped(tilesSkipped + other.getTilesSkipped());
    }
}
