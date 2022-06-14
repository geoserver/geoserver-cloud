/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoRemoveEvent;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ServiceInfoRemoveEvent.class, name = "ServiceInfoRemoved"),
    @JsonSubTypes.Type(value = SettingsInfoRemoveEvent.class, name = "SettingsInfoRemoved"),
})
public abstract class ConfigInfoRemoveEvent<SELF, INFO extends Info>
        extends InfoRemoveEvent<SELF, INFO> implements ConfigInfoEvent {

    private @Getter @Setter INFO object;

    protected ConfigInfoRemoveEvent() {
        // default constructor, needed for deserialization
    }

    public ConfigInfoRemoveEvent(@NonNull String objectId, @NonNull ConfigInfoType type) {
        super(objectId, type);
    }

    @SuppressWarnings("unchecked")
    public static @NonNull <I extends Info> ConfigInfoRemoveEvent<?, I> createLocal(
            @NonNull I info) {

        final ConfigInfoType type = ConfigInfoType.valueOf(info);
        switch (type) {
            case ServiceInfo:
                return (ConfigInfoRemoveEvent<?, I>)
                        ServiceInfoRemoveEvent.createLocal((ServiceInfo) info);
            case SettingsInfo:
                return (ConfigInfoRemoveEvent<?, I>)
                        SettingsInfoRemoveEvent.createLocal((SettingsInfo) info);
            default:
                throw new IllegalArgumentException(
                        "Uknown or unsupported config Info type: " + type + ". " + info);
        }
    }
}
