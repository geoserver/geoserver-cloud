/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.config.SettingsInfo;
import org.springframework.core.style.ToStringCreator;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("SettingsModified")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("serial")
public class SettingsModified extends ConfigInfoModified implements ConfigInfoEvent {

    private @Getter String workspaceId;

    protected SettingsModified() {
        // default constructor, needed for deserialization
    }

    public SettingsModified(
            long updateSequence, @NonNull SettingsInfo settings, @NonNull Patch patch, @NonNull String workspaceId) {

        super(updateSequence, resolveId(settings), prefixedName(settings), typeOf(settings), patch);
        this.workspaceId = workspaceId;
    }

    protected @Override ToStringCreator toStringBuilder() {
        return super.toStringBuilder().append("workspace", getWorkspaceId());
    }

    public static SettingsModified createLocal(
            long updateSequence, @NonNull SettingsInfo settings, @NonNull Patch patch) {

        final String workspaceId = InfoEvent.resolveId(settings.getWorkspace());

        return new SettingsModified(updateSequence, settings, patch, workspaceId);
    }
}
