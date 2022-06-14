/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.config.SettingsInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("SettingsInfoModified")
@EqualsAndHashCode(callSuper = true)
public class SettingsInfoModifyEvent
        extends ConfigInfoModifyEvent<SettingsInfoModifyEvent, SettingsInfo>
        implements ConfigInfoEvent {

    private @Getter @NonNull String workspaceId;

    protected SettingsInfoModifyEvent() {
        // default constructor, needed for deserialization
    }

    public SettingsInfoModifyEvent(
            @NonNull String objectId, @NonNull Patch patch, @NonNull String workspaceId) {

        super(objectId, ConfigInfoType.SettingsInfo, patch);
        this.workspaceId = workspaceId;
    }

    public @Override String toString() {
        return toStringBuilder().append("workspace", getWorkspaceId()).toString();
    }

    public static SettingsInfoModifyEvent createLocal(
            @NonNull SettingsInfo object, @NonNull Patch patch) {

        final String settingsId = object.getId();
        final String workspaceId = InfoEvent.resolveId(object.getWorkspace());

        return new SettingsInfoModifyEvent(settingsId, patch, workspaceId);
    }
}
