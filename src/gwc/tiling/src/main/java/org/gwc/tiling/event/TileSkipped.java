/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.event;

import lombok.NonNull;
import lombok.Value;

import org.geotools.tile.TileIdentifier;

/**
 * @since 1.0
 */
public @Value class TileSkipped implements TileEvent {

    private @NonNull TileIdentifier tile;
}
