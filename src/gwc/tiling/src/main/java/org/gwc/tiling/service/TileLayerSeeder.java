/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.service;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.gwc.tiling.model.MetaTileIdentifier;
import org.gwc.tiling.model.TileLayerInfo;

/**
 * Blocking worker for (re)seeding/truncating a tile layer
 *
 * @since 1.0
 */
@RequiredArgsConstructor
public abstract class TileLayerSeeder {

    private final @NonNull @Getter TileLayerInfo layer;

    /**
     * @return
     */
    public int getMetaWidth() {
        return layer.getMetaTilingWidth();
    }

    /**
     * @return
     */
    public int getMetaHeight() {
        return layer.getMetaTilingHeight();
    }

    /**
     * Ensures all tiles in the meta-tile {@link MetaTileIdentifier#getTiles() range} exists in the
     * cache, possibly disregarding the creation of tiles that already exist
     *
     * @param metatile
     */
    public abstract void seed(MetaTileRequest metatile);

    /**
     * Forces re-creating tiles in the meta-tile {@link MetaTileIdentifier#getTiles() range}
     *
     * @param metatile
     */
    public abstract void reseed(MetaTileRequest metatile);

    /**
     * @param metatile
     */
    public abstract void truncate(MetaTileRequest metatile);
}
