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
import org.geoserver.cloud.event.info.InfoRemoved;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ServiceRemoved.class, name = "ServiceInfoRemoved"),
    @JsonSubTypes.Type(value = SettingsRemoved.class, name = "SettingsInfoRemoved"),
})
@SuppressWarnings("serial")
public abstract class ConfigInfoRemoved<SELF, INFO extends Info> extends InfoRemoved<SELF, INFO>
        implements ConfigInfoEvent {

    protected ConfigInfoRemoved() {
        // default constructor, needed for deserialization
    }

    public ConfigInfoRemoved(
            long updateSequence, @NonNull String objectId, @NonNull ConfigInfoType type) {
        super(updateSequence, objectId, type);
    }

    @SuppressWarnings("unchecked")
    public static @NonNull <I extends Info> ConfigInfoRemoved<?, I> createLocal(
            long updateSequence, @NonNull I info) {

        final ConfigInfoType type = ConfigInfoType.valueOf(info);
        switch (type) {
            case ServiceInfo:
                return (ConfigInfoRemoved<?, I>)
                        ServiceRemoved.createLocal(updateSequence, (ServiceInfo) info);
            case SettingsInfo:
                return (ConfigInfoRemoved<?, I>)
                        SettingsRemoved.createLocal(updateSequence, (SettingsInfo) info);
            default:
                throw new IllegalArgumentException(
                        "Uknown or unsupported config Info type: " + type + ". " + info);
        }
    }
}
