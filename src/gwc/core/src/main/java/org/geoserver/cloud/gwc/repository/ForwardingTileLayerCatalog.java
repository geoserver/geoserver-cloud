/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.repository;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.geoserver.gwc.layer.TileLayerCatalog;

/** Base class for {@link TileLayerCatalog} decorators */
@RequiredArgsConstructor
public abstract class ForwardingTileLayerCatalog implements TileLayerCatalog {

    /** Note all {@link TileLayerCatalog} methods are delegated to this subject */
    @Getter
    @Delegate
    @NonNull
    protected final TileLayerCatalog delegate;
}
