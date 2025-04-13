/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
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
public abstract class ConfigInfoRemoved extends InfoRemoved implements ConfigInfoEvent {

    protected ConfigInfoRemoved() {
        // default constructor, needed for deserialization
    }

    protected ConfigInfoRemoved(
            long updateSequence, @NonNull String objectId, @NonNull String prefixedName, @NonNull ConfigInfoType type) {
        super(updateSequence, objectId, prefixedName, type);
    }

    public static @NonNull ConfigInfoRemoved createLocal(long updateSequence, @NonNull Info configInfo) {

        final ConfigInfoType type = ConfigInfoType.valueOf(configInfo);
        return switch (type) {
            case SERVICE -> ServiceRemoved.createLocal(updateSequence, (ServiceInfo) configInfo);
            case SETTINGS -> SettingsRemoved.createLocal(updateSequence, (SettingsInfo) configInfo);
            default ->
                throw new IllegalArgumentException(
                        "Unknown or unsupported config Info type: %s. %s".formatted(type, configInfo));
        };
    }
}
