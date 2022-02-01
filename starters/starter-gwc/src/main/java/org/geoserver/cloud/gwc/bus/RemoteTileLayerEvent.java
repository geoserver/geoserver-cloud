/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.bus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.geoserver.gwc.layer.TileLayerCatalogListener;
import org.springframework.context.ApplicationContext;

/**
 * Local {@link ApplicationContext} event issued to replace the tighly coupled {@link
 * TileLayerCatalogListener} by loosely coupled application events
 *
 * @since 1.0
 */
@NoArgsConstructor
public class RemoteTileLayerEvent extends RemoteGeoWebCacheEvent {

    private static final long serialVersionUID = 1L;

    private @Getter @Setter String layerId;

    public RemoteTileLayerEvent(Object source, @NonNull String originService) {
        super(source, originService);
    }

    protected @Override String getObjectId() {
        return layerId;
    }
}
