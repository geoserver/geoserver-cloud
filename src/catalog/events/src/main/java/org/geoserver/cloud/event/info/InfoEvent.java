/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.info;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Optional;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = InfoAdded.class),
    @JsonSubTypes.Type(value = InfoModified.class),
    @JsonSubTypes.Type(value = InfoRemoved.class)
})
@SuppressWarnings("serial")
public abstract class InfoEvent extends UpdateSequenceEvent {

    private @Getter @NonNull String objectId;
    private @Getter @NonNull String objectName;
    private @Getter @NonNull ConfigInfoType objectType;

    @SuppressWarnings("java:S2637")
    protected InfoEvent() {
        // no-op default constructor for deserialization
    }

    protected InfoEvent(
            long updateSequence,
            @NonNull String objectId,
            @NonNull String prefixedName,
            @NonNull ConfigInfoType objectType) {
        super(updateSequence);
        this.objectId = objectId;
        this.objectName = prefixedName;
        this.objectType = objectType;
    }

    protected @Override ToStringCreator toStringBuilder() {
        return super.toStringBuilder().append("type", getObjectType()).append("id", getObjectId());
    }

    /**
     * {@link #getObjectId() object identifier} for changes performed to the {@link Catalog} itself
     * (e.g. {@code defaultWorkspace} and the like)
     */
    protected static final String CATALOG_ID = "catalog";

    /**
     * {@link #getObjectId() object identifier} for changes performed to the {@link GeoServerInfo
     * global config} itself (e.g. {@code updateSequence} and the like)
     */
    private static final String GEOSERVER_ID = "geoserver";

    /**
     * {@link #getObjectId() object identifier} for changes performed to the {@link LoggingInfo}
     * config
     */
    private static final String LOGGING_ID = "logging";

    @Override
    public String toShortString() {
        String originService = getOrigin();
        String type = getClass().getSimpleName();
        return "%s[origin: %s, updateSequence: %s, object: %s(%s)]"
                .formatted(type, originService, getUpdateSequence(), getObjectType(), getObjectId());
    }

    @Nullable
    public static String resolveNullableId(@Nullable Info object) {
        if (null == object) return null;
        return resolveId(object);
    }

    @SuppressWarnings("java:S1481")
    public static String resolveId(@NonNull Info object) {
        return switch (object) {
            case Catalog c -> CATALOG_ID;
            case GeoServerInfo g -> GEOSERVER_ID;
            case LoggingInfo l -> LOGGING_ID;
            default -> object.getId();
        };
    }

    @NonNull
    @SuppressWarnings("java:S1481")
    public static String prefixedName(@NonNull Info object) {
        return switch (object) {
            case WorkspaceInfo w -> w.getName();
            case NamespaceInfo n -> n.getPrefix();
            case StoreInfo s -> prefixedName(s.getWorkspace(), s.getName());
            case ResourceInfo r -> prefixedName(r.getNamespace().getPrefix(), r.getName());
            case LayerInfo l -> prefixedName(l.getResource().getNamespace(), l.getName());
            case LayerGroupInfo lg -> prefixedName(lg.getWorkspace(), lg.getName());
            case StyleInfo s -> prefixedName(s.getWorkspace(), s.getName());
            case GeoServerInfo g -> GEOSERVER_ID;
            case LoggingInfo l -> LOGGING_ID;
            case Catalog c -> CATALOG_ID;
            case SettingsInfo s -> prefixedName(s.getWorkspace());
            case ServiceInfo s -> prefixedName(s.getWorkspace(), s.getName());
            default -> throw new IllegalArgumentException("Unknown object type for " + object);
        };
    }

    public static String prefixedName(WorkspaceInfo prefix, String name) {
        return prefixedName(prefix == null ? null : prefix.getName(), name);
    }

    public static String prefixedName(NamespaceInfo prefix, String name) {
        return prefixedName(prefix == null ? null : prefix.getPrefix(), name);
    }

    public static String prefixedName(Optional<String> prefix, String localName) {
        return prefixedName(prefix.orElse(null), localName);
    }

    private static String prefixedName(String prefix, String name) {
        return prefix == null ? name : "%s:%s".formatted(prefix, name);
    }

    public static Optional<String> prefix(@NonNull String prefixedName) {
        int idx = prefixedName.indexOf(':');
        return idx == -1 ? Optional.empty() : Optional.of(prefixedName.substring(0, idx));
    }

    public static String localName(@NonNull String prefixedName) {
        int idx = prefixedName.indexOf(':');
        return idx == -1 ? prefixedName : prefixedName.substring(1 + idx);
    }

    public static @NonNull ConfigInfoType typeOf(@NonNull Info info) {
        return ConfigInfoType.valueOf(info);
    }
}
