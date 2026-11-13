/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.resource;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.io.Serializable;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.platform.resource.Resource;
import org.springframework.core.style.ToStringCreator;

/**
 * Notifies the other service instances that a resource of the resource store was created, modified or deleted.
 *
 * <p>The {@code path} is the main resource affected. Other paths affected by the same operation are listed in
 * {@code changes}, for example a parent directory created on the way, a descendant deleted with a directory, or a child
 * moved with its parent.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("ResourceChanged")
@SuppressWarnings("serial")
public class ResourceChanged extends GeoServerEvent {

    public enum Kind {
        CREATED,
        MODIFIED,
        DELETED
    }

    /** One affected path besides the main one of the event. */
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class Change implements Serializable {
        private static final long serialVersionUID = 1L;

        private String path;
        private Kind kind;
    }

    private @Getter String path;
    private @Getter Kind kind;
    private @Getter Resource.Type type;
    private @Getter long mtime;
    // never null: an empty list is omitted from the JSON and read back as this default
    private @Getter List<Change> changes = List.of();

    protected ResourceChanged() {
        // no-op default constructor for deserialization
    }

    protected ResourceChanged(
            @NonNull String path,
            @NonNull Kind kind,
            @NonNull Resource.Type type,
            long mtime,
            @NonNull List<Change> changes) {
        super(System.currentTimeMillis(), resolveAuthor());
        this.path = path;
        this.kind = kind;
        this.type = type;
        this.mtime = mtime;
        this.changes = List.copyOf(changes);
    }

    public static ResourceChanged createLocal(
            @NonNull String path,
            @NonNull Kind kind,
            @NonNull Resource.Type type,
            long mtime,
            @NonNull List<Change> changes) {
        return new ResourceChanged(path, kind, type, mtime, changes);
    }

    protected @Override ToStringCreator toStringBuilder() {
        return super.toStringBuilder()
                .append("path", path)
                .append("kind", kind)
                .append("type", type)
                .append("mtime", mtime)
                .append("changes", changes);
    }

    @Override
    public String toShortString() {
        return "%s[origin: %s, %s %s]".formatted(getClass().getSimpleName(), getOrigin(), kind, path);
    }
}
