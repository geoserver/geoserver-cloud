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

import java.time.Instant;

/**
 * @since 1.0
 */
@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class CacheJobRequest {

    public static enum Action {
        SEED,
        RESEED,
        TRUNCATE;
    }

    private @NonNull Action action;
    private @NonNull CacheIdentifier cacheId;
    private @NonNull TilePyramid tiles;
    private @NonNull Instant timestamp;
}
