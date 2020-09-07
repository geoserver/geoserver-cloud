/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import lombok.NonNull;
import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.LocalPreModifyEvent;
import org.geoserver.cloud.event.PropertyDiff;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

public class LocalConfigPreModifyEvent extends LocalPreModifyEvent<GeoServer, Info> {

    private static final long serialVersionUID = 1L;

    public LocalConfigPreModifyEvent(
            @NonNull GeoServer source, Info info, @NonNull PropertyDiff diff) {
        super(source, info, diff);
    }

    public static LocalConfigPreModifyEvent of(
            @NonNull GeoServer source, @NonNull Info info, @NonNull PropertyDiff diff) {
        if (info instanceof GeoServerInfo
                || info instanceof SettingsInfo
                || info instanceof ServiceInfo
                || info instanceof LoggingInfo)
            return new LocalConfigPreModifyEvent(source, info, diff);

        throw new IllegalArgumentException(
                "Only GeoServerInfo, SettingsInfo, and ServiceInfo changes throw modify events, got "
                        + info.getClass().getSimpleName());
    }
}
