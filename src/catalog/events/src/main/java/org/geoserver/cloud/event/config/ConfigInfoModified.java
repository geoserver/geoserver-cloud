/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.NonNull;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoModified;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = GeoServerInfoModified.class),
    @JsonSubTypes.Type(value = LoggingInfoModified.class),
    @JsonSubTypes.Type(value = ServiceModified.class),
    @JsonSubTypes.Type(value = SettingsModified.class),
})
@SuppressWarnings("serial")
public abstract class ConfigInfoModified<I extends Info> extends InfoModified<I>
        implements ConfigInfoEvent {

    protected ConfigInfoModified() {
        // default constructor, needed for deserialization
    }

    protected ConfigInfoModified(
            long updateSequence,
            @NonNull String objectId,
            @NonNull ConfigInfoType objectType,
            @NonNull Patch patch) {
        super(updateSequence, objectId, objectType, patch);
    }

    @SuppressWarnings("unchecked")
    public static @NonNull <I extends Info> ConfigInfoModified<I> createLocal(
            long updateSequence, @NonNull Info info, @NonNull Patch patch) {

        final ConfigInfoType type = ConfigInfoType.valueOf(info);
        return (ConfigInfoModified<I>)
                switch (type) {
                    case GEOSERVER -> GeoServerInfoModified.createLocal(
                            updateSequence, (GeoServerInfo) info, patch);
                    case SERVICE -> ServiceModified.createLocal(
                            updateSequence, (ServiceInfo) info, patch);
                    case SETTINGS -> SettingsModified.createLocal(
                            updateSequence, (SettingsInfo) info, patch);
                    case LOGGING -> LoggingInfoModified.createLocal(
                            updateSequence, (LoggingInfo) info, patch);
                    default -> throw new IllegalArgumentException(
                            "Uknown or unsupported config Info type: " + type + ". " + info);
                };
    }
}
