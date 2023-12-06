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
import org.geoserver.cloud.event.info.InfoModified;

import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("CatalogInfoModified")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DefaultNamespaceSet.class),
    @JsonSubTypes.Type(value = DefaultWorkspaceSet.class),
    @JsonSubTypes.Type(value = DefaultDataStoreSet.class),
})
@SuppressWarnings("serial")
public class CatalogInfoModified extends InfoModified<CatalogInfo> {

    protected CatalogInfoModified() {}

    protected CatalogInfoModified(
            long updateSequence,
            @NonNull String objectId,
            @NonNull ConfigInfoType objectType,
            @NonNull Patch patch) {
        super(updateSequence, objectId, objectType, patch);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogInfoModified> remote() {
        return super.remote();
    }

    public static CatalogInfoModified createLocal(
            long updateSequence, @NonNull CatalogInfo info, @NonNull Patch patch) {

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

        return new CatalogInfoModified(updateSequence, resolveId(info), typeOf(info), patch);
    }

    public static CatalogInfoModified createLocal(
            long updateSequence, @NonNull CatalogPostModifyEvent event) {

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
                return DefaultWorkspaceSet.createLocal(updateSequence, ws);
            }
            Optional<Property> defaultNamespace = patch.get("defaultNamespace");
            if (defaultNamespace.isPresent()) {
                NamespaceInfo ns = (NamespaceInfo) defaultNamespace.get().getValue();
                return DefaultNamespaceSet.createLocal(updateSequence, ns);
            }
            if (patch.get("defaultDataStore").isPresent())
                return DefaultDataStoreSet.createLocal(updateSequence, event);

            throw new IllegalArgumentException(
                    "Catalog change events only support defaultWorkspace, defaultNamespace, and defaultDataStore properties. Diff: "
                            + patch);
        }

        return new CatalogInfoModified(updateSequence, resolveId(info), typeOf(info), patch);
    }
}
