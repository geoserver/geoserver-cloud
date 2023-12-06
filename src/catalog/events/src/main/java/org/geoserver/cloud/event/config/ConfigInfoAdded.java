/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.NonNull;

import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoAdded;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = GeoServerInfoSet.class),
    @JsonSubTypes.Type(value = LoggingInfoSet.class),
    @JsonSubTypes.Type(value = ServiceAdded.class),
    @JsonSubTypes.Type(value = SettingsAdded.class),
})
@SuppressWarnings("serial")
public abstract class ConfigInfoAdded<INFO extends Info> extends InfoAdded<INFO>
        implements ConfigInfoEvent {

    protected ConfigInfoAdded() {
        // default constructor, needed for deserialization
    }

    protected ConfigInfoAdded(long updateSequence, INFO object) {
        super(updateSequence, object);
    }

    @SuppressWarnings("unchecked")
    public static @NonNull <I extends Info> ConfigInfoAdded<I> createLocal(
            long updateSequence, @NonNull I info) {

        final ConfigInfoType type = ConfigInfoType.valueOf(info);
        switch (type) {
            case GeoServerInfo:
                return (ConfigInfoAdded<I>)
                        GeoServerInfoSet.createLocal(updateSequence, (GeoServerInfo) info);
            case ServiceInfo:
                return (ConfigInfoAdded<I>)
                        ServiceAdded.createLocal(updateSequence, (ServiceInfo) info);
            case SettingsInfo:
                return (ConfigInfoAdded<I>)
                        SettingsAdded.createLocal(updateSequence, (SettingsInfo) info);
            case LoggingInfo:
                return (ConfigInfoAdded<I>)
                        LoggingInfoSet.createLocal(updateSequence, (LoggingInfo) info);
            default:
                throw new IllegalArgumentException(
                        "Uknown or unsupported config Info type: " + type + ". " + info);
        }
    }
}
