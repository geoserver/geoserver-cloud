/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import lombok.NonNull;
import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.LocalPostModifyEvent;
import org.geoserver.cloud.event.PropertyDiff;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

public class LocalConfigPostModifyEvent extends LocalPostModifyEvent<GeoServer, Info> {

    private static final long serialVersionUID = 1L;

    LocalConfigPostModifyEvent(GeoServer source, Info info, PropertyDiff diff) {
        super(source, info, diff);
    }

    public static LocalConfigPostModifyEvent of(
            @NonNull GeoServer source, @NonNull Info info, @NonNull PropertyDiff diff) {
        if (info instanceof GeoServerInfo
                || info instanceof SettingsInfo
                || info instanceof ServiceInfo
                || info instanceof LoggingInfo)
            return new LocalConfigPostModifyEvent(source, info, diff);

        throw new IllegalArgumentException(
                "Only GeoServerInfo, SettingsInfo, LoggingInfo, and ServiceInfo changes throw modify events, got "
                        + info.getClass().getSimpleName());
    }
}
