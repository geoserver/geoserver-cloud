/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.gwc;

import lombok.Getter;
import lombok.Setter;

import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;

/**
 * @since 1.4
 */
@SuppressWarnings("serial")
public class PgsqlTileLayerInfo extends GeoServerTileLayerInfoImpl {

    @Getter @Setter private String workspaceId;

    @Override
    public GeoServerTileLayerInfoImpl clone() {
        PgsqlTileLayerInfo clone = (PgsqlTileLayerInfo) super.clone();
        return clone;
    }
}
