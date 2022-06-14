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
import org.geoserver.cloud.event.info.InfoAddEvent;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = GeoServerInfoSetEvent.class, name = "GeoServerInfoSet"),
    @JsonSubTypes.Type(value = LoggingInfoSetEvent.class, name = "LoggingInfoSet"),
    @JsonSubTypes.Type(value = ServiceInfoAddEvent.class, name = "ServiceInfoAdded"),
    @JsonSubTypes.Type(value = SettingsInfoAddEvent.class, name = "SettingsInfoAdded"),
})
public abstract class ConfigInfoAddEvent<SELF, INFO extends Info> extends InfoAddEvent<SELF, INFO>
        implements ConfigInfoEvent {

    protected ConfigInfoAddEvent() {
        // default constructor, needed for deserialization
    }

    public ConfigInfoAddEvent(INFO object) {
        super(object);
    }

    @SuppressWarnings("unchecked")
    public static @NonNull <I extends Info> ConfigInfoAddEvent<?, I> createLocal(@NonNull I info) {

        final ConfigInfoType type = ConfigInfoType.valueOf(info);
        switch (type) {
            case GeoServerInfo:
                return (ConfigInfoAddEvent<?, I>)
                        GeoServerInfoSetEvent.createLocal((GeoServerInfo) info);
            case ServiceInfo:
                return (ConfigInfoAddEvent<?, I>)
                        ServiceInfoAddEvent.createLocal((ServiceInfo) info);
            case SettingsInfo:
                return (ConfigInfoAddEvent<?, I>)
                        SettingsInfoAddEvent.createLocal((SettingsInfo) info);
            case LoggingInfo:
                return (ConfigInfoAddEvent<?, I>)
                        LoggingInfoSetEvent.createLocal((LoggingInfo) info);
            default:
                throw new IllegalArgumentException(
                        "Uknown or unsupported config Info type: " + type + ". " + info);
        }
    }
}
