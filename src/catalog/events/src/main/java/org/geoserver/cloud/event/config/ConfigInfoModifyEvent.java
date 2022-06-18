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
import org.geoserver.cloud.event.info.InfoPostModifyEvent;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = GeoServerInfoModifyEvent.class),
    @JsonSubTypes.Type(value = LoggingInfoModifyEvent.class),
    @JsonSubTypes.Type(value = ServiceInfoModifyEvent.class),
    @JsonSubTypes.Type(value = SettingsInfoModifyEvent.class),
})
public abstract class ConfigInfoModifyEvent<SELF, INFO extends Info>
        extends InfoPostModifyEvent<SELF, INFO> implements ConfigInfoEvent {

    protected ConfigInfoModifyEvent() {
        // default constructor, needed for deserialization
    }

    protected ConfigInfoModifyEvent(
            @NonNull String objectId, @NonNull ConfigInfoType objectType, @NonNull Patch patch) {
        super(objectId, objectType, patch);
    }

    @SuppressWarnings("unchecked")
    public static @NonNull <I extends Info> ConfigInfoModifyEvent<?, I> createLocal(
            @NonNull Info info, @NonNull Patch patch) {

        final ConfigInfoType type = ConfigInfoType.valueOf(info);
        switch (type) {
            case GeoServerInfo:
                {
                    return (ConfigInfoModifyEvent<?, I>)
                            GeoServerInfoModifyEvent.createLocal((GeoServerInfo) info, patch);
                }
            case ServiceInfo:
                ServiceInfo service = (ServiceInfo) info;
                return (ConfigInfoModifyEvent<?, I>)
                        ServiceInfoModifyEvent.createLocal(service, patch);
            case SettingsInfo:
                SettingsInfo settings = (SettingsInfo) info;
                return (ConfigInfoModifyEvent<?, I>)
                        SettingsInfoModifyEvent.createLocal(settings, patch);
            case LoggingInfo:
                return (ConfigInfoModifyEvent<?, I>)
                        LoggingInfoModifyEvent.createLocal((LoggingInfo) info, patch);
            default:
                throw new IllegalArgumentException(
                        "Uknown or unsupported config Info type: " + type + ". " + info);
        }
    }
}
