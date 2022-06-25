/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.catalog;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.catalog.plugin.PropertyDiff.Change;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.springframework.core.style.ToStringCreator;

import java.util.Objects;

import javax.annotation.Nullable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("DefaultDataStoreSet")
@EqualsAndHashCode(callSuper = true)
public class DefaultDataStoreSet extends CatalogInfoModified {

    private @NonNull @Getter String workspaceId;
    private @Getter String defaultDataStoreId;

    protected DefaultDataStoreSet() {}

    DefaultDataStoreSet(
            @NonNull Long updateSequence,
            @NonNull String workspaceId,
            String defaultDataStoreId,
            @NonNull Patch patch) {

        super(updateSequence, InfoEvent.CATALOG_ID, ConfigInfoType.Catalog, patch);

        this.workspaceId = workspaceId;
        this.defaultDataStoreId = defaultDataStoreId;
    }

    protected @Override ToStringCreator toStringBuilder() {
        return super.toStringBuilder()
                .append("workspace", getWorkspaceId())
                .append("store", getDefaultDataStoreId());
    }

    public static DefaultDataStoreSet createLocal(
            @NonNull Long updateSequence, @NonNull CatalogPostModifyEvent event) {

        PropertyDiff diff =
                PropertyDiff.valueOf(
                        event.getPropertyNames(), event.getOldValues(), event.getNewValues());

        final Change change =
                diff.get("defaultDataStore")
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "defaultDataStore is not in the change list"));

        final @Nullable DataStoreInfo newStore = (DataStoreInfo) change.getNewValue();

        final @Nullable String newDefaultStoreId = resolveId(newStore);
        final @NonNull String workspaceId = resolveId(resolveWorkspace(change));

        Patch patch = diff.toPatch();

        return new DefaultDataStoreSet(updateSequence, workspaceId, newDefaultStoreId, patch);
    }

    public static DefaultDataStoreSet createLocal(
            @NonNull Long updateSequence,
            @NonNull WorkspaceInfo workspace,
            DataStoreInfo newStore) {

        Patch patch = new Patch();
        patch.add("defaultDataStore", newStore);

        final @Nullable String newDefaultStoreId = resolveId(newStore);
        final @NonNull String workspaceId = resolveId(workspace);

        return new DefaultDataStoreSet(updateSequence, workspaceId, newDefaultStoreId, patch);
    }

    private static @NonNull WorkspaceInfo resolveWorkspace(Change change) {
        DataStoreInfo oldStore = (DataStoreInfo) change.getOldValue();
        DataStoreInfo newStore = (DataStoreInfo) change.getNewValue();
        DataStoreInfo store = oldStore == null ? newStore : oldStore;
        Objects.requireNonNull(store);
        return store.getWorkspace();
    }
}
