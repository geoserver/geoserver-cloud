/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import lombok.NonNull;

import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.LocalAddEvent;
import org.geoserver.config.GeoServer;

public class LocalConfigAddEvent extends LocalAddEvent<GeoServer, Info> {
    private static final long serialVersionUID = 1L;

    LocalConfigAddEvent(GeoServer source, Info object) {
        super(source, object);
    }

    public static LocalConfigAddEvent of(@NonNull GeoServer source, @NonNull Info info) {
        return new LocalConfigAddEvent(source, info);
    }
}
