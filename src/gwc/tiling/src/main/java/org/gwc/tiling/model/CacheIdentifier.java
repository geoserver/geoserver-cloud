/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Optional;

/**
 * @since 1.0
 */
@Value
@Builder
@Jacksonized
public class CacheIdentifier {

    private @NonNull String layerName;
    private @NonNull String gridsetId;
    private @NonNull String format;
    private @NonNull Optional<String> parametersId;
}
