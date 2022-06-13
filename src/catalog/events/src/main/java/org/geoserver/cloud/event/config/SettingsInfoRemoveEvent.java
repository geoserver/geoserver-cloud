/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Getter;
import lombok.NonNull;

import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("SettingsInfoRemoved")
public class SettingsInfoRemoveEvent
        extends ConfigInfoRemoveEvent<SettingsInfoRemoveEvent, SettingsInfo> {

    private @Getter @NonNull String workspaceId;

    protected SettingsInfoRemoveEvent() {
        // default constructor, needed for deserialization
    }

    protected SettingsInfoRemoveEvent(
            GeoServer source,
            GeoServer target,
            @NonNull String objectId,
            @NonNull String workspaceId) {

        super(source, target, objectId, ConfigInfoType.SettingsInfo);
        this.workspaceId = workspaceId;
    }

    public static SettingsInfoRemoveEvent createLocal(
            @NonNull GeoServer source, @NonNull SettingsInfo settings) {

        final @NonNull String settingsId = settings.getId();
        final @NonNull String workspaceId = settings.getWorkspace().getId();

        return new SettingsInfoRemoveEvent(source, null, settingsId, workspaceId);
    }
}
