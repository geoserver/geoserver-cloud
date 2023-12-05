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
import org.geoserver.config.SettingsInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("SettingsInfoRemoved")
@SuppressWarnings("serial")
public class SettingsRemoved extends ConfigInfoRemoved<SettingsInfo> {

    private @Getter String workspaceId;

    protected SettingsRemoved() {
        // default constructor, needed for deserialization
    }

    protected SettingsRemoved(
            long updateSequence, @NonNull String objectId, @NonNull String workspaceId) {

        super(updateSequence, objectId, ConfigInfoType.SettingsInfo);
        this.workspaceId = workspaceId;
    }

    public static SettingsRemoved createLocal(long updateSequence, @NonNull SettingsInfo settings) {

        final @NonNull String settingsId = settings.getId();
        final @NonNull String workspaceId = settings.getWorkspace().getId();

        return new SettingsRemoved(updateSequence, settingsId, workspaceId);
    }
}
