/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.NonNull;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoPreModifyEvent;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("ConfigInfoPreModify")
public class ConfigInfoPreModifyEvent<SELF, INFO extends Info>
        extends InfoPreModifyEvent<SELF, INFO> implements ConfigInfoEvent {

    protected ConfigInfoPreModifyEvent() {
        // default constructor, needed for deserialization
    }

    protected ConfigInfoPreModifyEvent(
            @NonNull String objectId, @NonNull ConfigInfoType objectType, @NonNull Patch patch) {
        super(objectId, objectType, patch);
    }

    public static @NonNull <I extends Info> ConfigInfoPreModifyEvent<?, I> createLocal(
            @NonNull Info object, @NonNull PropertyDiff diff) {

        final @NonNull String id = resolveId(object);
        final ConfigInfoType type = ConfigInfoType.valueOf(object);
        final Patch patch = diff.toPatch();
        return new ConfigInfoPreModifyEvent<>(id, type, patch);
    }
}
