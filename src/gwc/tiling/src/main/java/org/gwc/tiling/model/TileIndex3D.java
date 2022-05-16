/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * @since 1.0
 */
@Value
@Builder
@Jacksonized
public class TileIndex3D implements Comparable<TileIndex3D> {

    private @NonNull TileIndex2D index;
    private int z;

    public long getX() {
        return index.getX();
    }

    public long getY() {
        return index.getY();
    }

    @Override
    public int compareTo(TileIndex3D o) {
        int c = Integer.compare(z, o.getZ());
        if (c == 0) {
            c = index.compareTo(o.getIndex());
        }
        return c;
    }
}
