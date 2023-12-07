/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.catalog;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.springframework.core.style.ToStringCreator;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("DefaultWorkspaceSet")
@SuppressWarnings("serial")
public class DefaultWorkspaceSet extends CatalogInfoModified {

    private @Getter @Setter String newWorkspaceId;

    /** default constructor, needed for deserialization */
    protected DefaultWorkspaceSet() {
        //
    }

    DefaultWorkspaceSet(long updateSequence, String newWorkspaceId, @NonNull Patch patch) {
        super(updateSequence, InfoEvent.CATALOG_ID, ConfigInfoType.CATALOG, patch);
        this.newWorkspaceId = newWorkspaceId;
    }

    protected @Override ToStringCreator toStringBuilder() {
        return super.toStringBuilder().append("workspace", getNewWorkspaceId());
    }

    public static DefaultWorkspaceSet createLocal(
            long updateSequence, WorkspaceInfo defaultWorkspace) {

        String workspaceId = resolveId(defaultWorkspace);
        Patch patch = new Patch();
        patch.add("defaultWorkspace", defaultWorkspace);
        return new DefaultWorkspaceSet(updateSequence, workspaceId, patch);
    }
}
