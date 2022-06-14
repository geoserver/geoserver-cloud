/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.info;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.springframework.core.style.ToStringCreator;

import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = InfoAddEvent.class),
    @JsonSubTypes.Type(value = InfoModifyEvent.class),
    @JsonSubTypes.Type(value = InfoRemoveEvent.class)
})
public abstract class InfoEvent<SELF, INFO extends Info> {

    private @Setter boolean remote;

    /** System time when the event happened. */
    private @Getter long timestamp;

    private @Getter String objectId;

    private @Getter ConfigInfoType objectType;

    protected InfoEvent() {}

    protected InfoEvent(@NonNull String objectId, @NonNull ConfigInfoType objectType) {
        // this.source = source;
        this.objectId = objectId;
        this.objectType = objectType;
        this.timestamp = System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    public Optional<SELF> local() {
        return Optional.ofNullable(isLocal() ? (SELF) this : null);
    }

    @SuppressWarnings("unchecked")
    public Optional<SELF> remote() {
        return Optional.ofNullable(isRemote() ? (SELF) this : null);
    }

    public boolean isLocal() {
        return !isRemote(); // source != null;
    }

    public boolean isRemote() {
        return remote;
    }

    public @Override String toString() {
        return toStringBuilder().toString();
    }

    protected ToStringCreator toStringBuilder() {
        return new ToStringCreator(this)
                .append("remote", isRemote())
                .append("type", getObjectType())
                .append("id", getObjectId());
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

    public static String resolveId(Info object) {
        if (null == object) return null;
        String id = object.getId();
        if (null != id) return id;

        if (object instanceof Catalog) return CATALOG_ID;
        if (object instanceof GeoServerInfo) return GEOSERVER_ID;
        if (object instanceof LoggingInfo) return LOGGING_ID;

        throw new IllegalStateException();
    }

    public static @NonNull ConfigInfoType typeOf(@NonNull Info info) {
        return ConfigInfoType.valueOf(info);
    }
}
