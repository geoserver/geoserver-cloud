/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import lombok.NonNull;

import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.LocalRemoveEvent;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

public class LocalConfigRemoveEvent extends LocalRemoveEvent<GeoServer, Info> {
    private static final long serialVersionUID = 1L;

    public LocalConfigRemoveEvent(GeoServer source, @NonNull Info object) {
        super(source, object);
    }

    public static LocalConfigRemoveEvent of(GeoServer source, @NonNull SettingsInfo info) {
        return new LocalConfigRemoveEvent(source, info);
    }

    public static LocalConfigRemoveEvent of(GeoServer source, @NonNull ServiceInfo info) {
        return new LocalConfigRemoveEvent(source, info);
    }
}
