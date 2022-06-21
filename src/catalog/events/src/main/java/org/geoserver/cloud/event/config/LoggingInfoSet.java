/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.NonNull;

import org.geoserver.config.GeoServer;
import org.geoserver.config.LoggingInfo;

/** Event sent when {@link GeoServer#setLogging(LoggingInfo)} is called on a node */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("LoggingInfoSet")
public class LoggingInfoSet extends ConfigInfoAdded<LoggingInfoSet, LoggingInfo>
        implements ConfigInfoEvent {

    protected LoggingInfoSet() {
        // default constructor, needed for deserialization
    }

    protected LoggingInfoSet(@NonNull Long updateSequence, LoggingInfo object) {
        super(updateSequence, object);
    }

    public static LoggingInfoSet createLocal(@NonNull Long updateSequence, LoggingInfo value) {
        return new LoggingInfoSet(updateSequence, value);
    }
}
