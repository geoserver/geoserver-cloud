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
import org.springframework.core.style.ToStringCreator;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("SettingsModified")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("serial")
public class SettingsModified extends ConfigInfoModified<SettingsModified, SettingsInfo>
        implements ConfigInfoEvent {

    private @Getter String workspaceId;

    protected SettingsModified() {
        // default constructor, needed for deserialization
    }

    public SettingsModified(
            long updateSequence,
            @NonNull String objectId,
            @NonNull Patch patch,
            @NonNull String workspaceId) {

        super(updateSequence, objectId, ConfigInfoType.SettingsInfo, patch);
        this.workspaceId = workspaceId;
    }

    protected @Override ToStringCreator toStringBuilder() {
        return super.toStringBuilder().append("workspace", getWorkspaceId());
    }

    public static SettingsModified createLocal(
            long updateSequence, @NonNull SettingsInfo object, @NonNull Patch patch) {

        final String settingsId = object.getId();
        final String workspaceId = InfoEvent.resolveId(object.getWorkspace());

        return new SettingsModified(updateSequence, settingsId, patch, workspaceId);
    }
}
