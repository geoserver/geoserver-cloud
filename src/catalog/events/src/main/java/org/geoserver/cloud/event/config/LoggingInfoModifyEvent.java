/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.LoggingInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("LoggingInfoModified")
@EqualsAndHashCode(callSuper = true)
public class LoggingInfoModifyEvent
        extends ConfigInfoModifyEvent<LoggingInfoModifyEvent, LoggingInfo>
        implements ConfigInfoEvent {

    protected LoggingInfoModifyEvent() {
        // default constructor, needed for deserialization
    }

    protected LoggingInfoModifyEvent(
            GeoServer source, GeoServer target, @NonNull String id, @NonNull Patch patch) {

        super(source, target, id, ConfigInfoType.LoggingInfo, patch);
    }

    public static LoggingInfoModifyEvent createLocal(
            GeoServer source, LoggingInfo info, @NonNull Patch patch) {
        String id = resolveId(info);
        return new LoggingInfoModifyEvent(source, null, id, patch);
    }
}
