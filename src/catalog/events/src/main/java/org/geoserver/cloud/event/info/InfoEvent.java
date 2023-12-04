/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.info;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.springframework.core.style.ToStringCreator;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = InfoAdded.class),
    @JsonSubTypes.Type(value = InfoModified.class),
    @JsonSubTypes.Type(value = InfoRemoved.class)
})
public abstract class InfoEvent<SELF, INFO extends Info> extends UpdateSequenceEvent<SELF> {

    private @Getter String objectId;

    private @Getter ConfigInfoType objectType;

    protected InfoEvent() {}

    protected InfoEvent(
            long updateSequence, @NonNull String objectId, @NonNull ConfigInfoType objectType) {
        super(updateSequence);
        this.objectId = objectId;
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
