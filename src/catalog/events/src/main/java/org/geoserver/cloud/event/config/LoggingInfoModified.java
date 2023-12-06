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
import org.geoserver.config.LoggingInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("LoggingInfoModified")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("serial")
public class LoggingInfoModified extends ConfigInfoModified<LoggingInfo>
        implements ConfigInfoEvent {

    protected LoggingInfoModified() {
        // default constructor, needed for deserialization
    }

    protected LoggingInfoModified(long updateSequence, @NonNull String id, @NonNull Patch patch) {

        super(updateSequence, id, ConfigInfoType.LoggingInfo, patch);
    }

    public static LoggingInfoModified createLocal(
            long updateSequence, LoggingInfo info, @NonNull Patch patch) {
        String id = resolveId(info);
        return new LoggingInfoModified(updateSequence, id, patch);
    }
}
