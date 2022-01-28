/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.event;

import lombok.NonNull;

/** @since 1.0 */
public class TileLayerAddedEvent extends TileLayerEvent {

    private static final long serialVersionUID = 1L;

    public TileLayerAddedEvent(Object source, @NonNull String layerId) {
        super(source, layerId, Type.CREATE);
    }
}
