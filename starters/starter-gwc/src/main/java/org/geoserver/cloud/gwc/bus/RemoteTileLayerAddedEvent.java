/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.bus;

import lombok.NoArgsConstructor;
import lombok.NonNull;

/** @since 1.0 */
@NoArgsConstructor
public class RemoteTileLayerAddedEvent extends RemoteTileLayerEvent {

    private static final long serialVersionUID = 1L;

    public RemoteTileLayerAddedEvent(
            Object source, @NonNull String originService, @NonNull String layerId) {
        super(source, originService, layerId, Type.CREATED);
    }
}
