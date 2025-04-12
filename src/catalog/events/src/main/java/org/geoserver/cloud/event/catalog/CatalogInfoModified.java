/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.catalog;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.annotations.VisibleForTesting;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Patch.Property;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.catalog.plugin.PropertyDiff.Change;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoModified;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("CatalogInfoModified")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DefaultNamespaceSet.class),
    @JsonSubTypes.Type(value = DefaultWorkspaceSet.class),
    @JsonSubTypes.Type(value = DefaultDataStoreSet.class),
})
@SuppressWarnings("serial")
public class CatalogInfoModified extends InfoModified {

    private @NonNull @Getter String oldName;

    @SuppressWarnings("java:S2637")
    protected CatalogInfoModified() {
        // no-op default constructor for deserialization
    }

    protected CatalogInfoModified(
            long updateSequence,
            @NonNull String objectId,
            @NonNull String prefixedName,
            @NonNull String oldName,
            @NonNull ConfigInfoType objectType,
            @NonNull Patch patch) {
        super(updateSequence, objectId, prefixedName, objectType, patch);
        this.oldName = oldName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogInfoModified> remote() {
        return super.remote();
    }

    @VisibleForTesting
    public static CatalogInfoModified createLocal(long updateSequence, @NonNull CatalogInfo modProxy) {

        ModificationProxy proxy =
                Objects.requireNonNull(ModificationProxy.handler(modProxy), "Argument is not a ModificationProxy");
        PropertyDiff diff = PropertyDiff.valueOf(proxy);
        Patch patch = diff.toPatch();

        if (modProxy instanceof Catalog) {
            if (patch.get("defaultWorkspace").isPresent()) {
                throw new IllegalArgumentException("Use DefaultWorkspaceEvent.createLocal()");
            }
            if (patch.get("defaultNamespace").isPresent()) {
                throw new IllegalArgumentException("Use DefaultNamespaceStoreEvent.createLocal()");
            }
            if (patch.get("defaultDataStore").isPresent()) {
                throw new IllegalArgumentException("Use DefaultDataStoreEvent.createLocal()");
            }
            throw new IllegalArgumentException(
                    "Catalog change events only support defaultWorkspace, defaultNamespace, and defaultDataStore properties. Diff: %s"
                            .formatted(patch));
        }

        String id = resolveId(modProxy);
        @NonNull String prefixedName = prefixedName(modProxy);
        @NonNull ConfigInfoType type = typeOf(modProxy);
        @NonNull String oldName = oldName(diff, type, prefixedName);
        return new CatalogInfoModified(updateSequence, id, prefixedName, oldName, type, patch);
    }

    public static CatalogInfoModified createLocal(long updateSequence, @NonNull CatalogPostModifyEvent event) {

        final CatalogInfo info = event.getSource();
        PropertyDiff diff = PropertyDiff.valueOf(event.getPropertyNames(), event.getOldValues(), event.getNewValues())
                .clean();
        final Patch patch = diff.toPatch();

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
            if (patch.get("defaultDataStore").isPresent()) {
                return DefaultDataStoreSet.createLocal(updateSequence, event);
            }
            throw new IllegalArgumentException(
                    "Catalog change events only support defaultWorkspace, defaultNamespace, and defaultDataStore properties. Diff: %s"
                            .formatted(patch));
        }

        String id = resolveId(info);
        @NonNull String prefixedName = prefixedName(info);
        @NonNull ConfigInfoType type = typeOf(info);
        @NonNull String oldName = oldName(diff, type, prefixedName);
        return new CatalogInfoModified(updateSequence, id, prefixedName, oldName, type, patch);
    }

    private static @NonNull String oldName(
            PropertyDiff diff, @NonNull ConfigInfoType type, @NonNull String defaultValue) {

        Optional<Change> change = diff.get("name");
        if (change.isEmpty() && type == ConfigInfoType.NAMESPACE) {
            change = diff.get("prefix");
        }
        return change.map(Change::getOldValue).map(String.class::cast).orElse(defaultValue);
    }
}
