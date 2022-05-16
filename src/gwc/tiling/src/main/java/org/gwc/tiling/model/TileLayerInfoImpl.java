/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @since 1.0
 */
@Value
@Builder
@Jacksonized
public class TileLayerInfoImpl implements TileLayerInfo {

    private @NonNull String name;
    private @NonNull Set<String> formats;
    private @NonNull List<GridSubsetInfo> gridSubsets;
    private int metaTilingWidth;
    private int metaTilingHeight;

    @Override
    public Set<String> gridSubsetNames() {
        return gridSubsets.stream().map(GridSubsetInfo::getName).collect(Collectors.toSet());
    }

    @Override
    public Optional<GridSubsetInfo> gridSubset(@NonNull String gridsetId) {
        return gridSubsets.stream().filter(gs -> gridsetId.equals(gs.getName())).findFirst();
    }
}
