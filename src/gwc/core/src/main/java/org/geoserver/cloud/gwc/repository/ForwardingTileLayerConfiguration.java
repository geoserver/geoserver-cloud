/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.repository;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import org.geowebcache.config.TileLayerConfiguration;

/** Base class for {@link TileLayerConfiguration} decorators */
@RequiredArgsConstructor
public class ForwardingTileLayerConfiguration implements TileLayerConfiguration {

    /** Note all {@link TileLayerConfiguration} methods are delegated to this subject */
    @Getter @Delegate @NonNull private final TileLayerConfiguration subject;
}
