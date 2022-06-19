/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.catalog;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Patch.Property;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoPostModifyEvent;

import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("CatalogInfoModified")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DefaultNamespaceEvent.class, name = "DefaultNamespaceSet"),
    @JsonSubTypes.Type(value = DefaultWorkspaceEvent.class, name = "DefaultWorkspaceSet"),
    @JsonSubTypes.Type(value = DefaultDataStoreEvent.class, name = "DefaultDataStoreSet"),
})
public class CatalogInfoModifyEvent
        extends InfoPostModifyEvent<CatalogInfoModifyEvent, CatalogInfo> {

    protected CatalogInfoModifyEvent() {}

    protected CatalogInfoModifyEvent(
            @NonNull String objectId, @NonNull ConfigInfoType objectType, @NonNull Patch patch) {
        super(objectId, objectType, patch);
    }

    public static CatalogInfoModifyEvent createLocal(
            @NonNull CatalogInfo info, @NonNull Patch patch) {

        if (info instanceof Catalog) {
            if (patch.get("defaultWorkspace").isPresent()) {
                throw new IllegalArgumentException("Use DefaultWorkspaceEvent.createLocal()");
            }
            if (patch.get("defaultNamespace").isPresent()) {
                throw new IllegalArgumentException("Use DefaultNamespaceStoreEvent.createLocal()");
            }
            if (patch.get("defaultDataStore").isPresent())
                throw new IllegalArgumentException("Use DefaultDataStoreEvent.createLocal()");
            throw new IllegalArgumentException(
                    "Catalog change events only support defaultWorkspace, defaultNamespace, and defaultDataStore properties. Diff: "
                            + patch);
        }

        return new CatalogInfoModifyEvent(resolveId(info), typeOf(info), patch);
    }

    public static CatalogInfoModifyEvent createLocal(@NonNull CatalogPostModifyEvent event) {

        final CatalogInfo info = event.getSource();
        final Patch patch =
                PropertyDiff.valueOf(
                                event.getPropertyNames(),
                                event.getOldValues(),
                                event.getNewValues())
                        .clean()
                        .toPatch();

        if (!patch.isEmpty() && info instanceof Catalog) {
            Optional<Property> defaultWorkspace = patch.get("defaultWorkspace");
            if (defaultWorkspace.isPresent()) {
                WorkspaceInfo ws = (WorkspaceInfo) defaultWorkspace.get().getValue();
                return DefaultWorkspaceEvent.createLocal(ws);
            }
            Optional<Property> defaultNamespace = patch.get("defaultNamespace");
            if (defaultNamespace.isPresent()) {
                NamespaceInfo ns = (NamespaceInfo) defaultNamespace.get().getValue();
                return DefaultNamespaceEvent.createLocal(ns);
            }
            if (patch.get("defaultDataStore").isPresent())
                return DefaultDataStoreEvent.createLocal(event);

            throw new IllegalArgumentException(
                    "Catalog change events only support defaultWorkspace, defaultNamespace, and defaultDataStore properties. Diff: "
                            + patch);
        }

        return new CatalogInfoModifyEvent(resolveId(info), typeOf(info), patch);
    }
}
