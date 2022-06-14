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

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("DefaultWorkspaceSet")
public class DefaultWorkspaceEvent extends CatalogInfoModifyEvent {

    private @Getter @Setter String newWorkspaceId;

    /** default constructor, needed for deserialization */
    protected DefaultWorkspaceEvent() {
        //
    }

    DefaultWorkspaceEvent(String newWorkspaceId, @NonNull Patch patch) {
        super(InfoEvent.CATALOG_ID, ConfigInfoType.Catalog, patch);
        this.newWorkspaceId = newWorkspaceId;
    }

    public @Override String toString() {
        return toStringBuilder().append("workspace", getNewWorkspaceId()).toString();
    }

    public static DefaultWorkspaceEvent createLocal(WorkspaceInfo defaultWorkspace) {

        String workspaceId = resolveId(defaultWorkspace);
        Patch patch = new Patch();
        patch.add("defaultWorkspace", defaultWorkspace);
        return new DefaultWorkspaceEvent(workspaceId, patch);
    }
}
