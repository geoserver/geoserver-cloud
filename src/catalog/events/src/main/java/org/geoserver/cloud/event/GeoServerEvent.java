/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import java.util.Optional;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.lifecycle.LifecycleEvent;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.springframework.core.style.ToStringCreator;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({@JsonSubTypes.Type(value = UpdateSequenceEvent.class), @JsonSubTypes.Type(value = LifecycleEvent.class)})
@SuppressWarnings("serial")
public abstract class GeoServerEvent implements Serializable {

    @JsonIgnore
    private @Setter boolean remote;

    @JsonIgnore
    private @Setter @Getter String origin;

    /** System time when the event happened. */
    private @Getter long timestamp;

    private @Getter String author;

    private @Getter @Setter String id;

    protected GeoServerEvent() {}

    protected GeoServerEvent(long timestamp, String author) {
        this.timestamp = timestamp;
        this.author = author;
    }

    @SuppressWarnings("unchecked")
    public <T extends GeoServerEvent> Optional<T> local() {
        return Optional.ofNullable(isLocal() ? (T) this : null);
    }

    @SuppressWarnings("unchecked")
    public <T extends GeoServerEvent> Optional<T> remote() {
        return Optional.ofNullable(isRemote() ? (T) this : null);
    }

    public boolean isLocal() {
        return !isRemote();
    }

    public boolean isRemote() {
        return remote;
    }

    @Override
    public String toString() {
        return toStringBuilder().toString();
    }

    public abstract String toShortString();

    protected ToStringCreator toStringBuilder() {
        return new ToStringCreator(this)
                .append("id", getId())
                .append("remote", isRemote())
                .append("origin", getOrigin())
                .append("author", getAuthor());
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
        if (null == object) {
            return null;
        }
        String id = object.getId();
        if (null != id) {
            return id;
        }
        if (object instanceof Catalog) {
            return CATALOG_ID;
        }
        if (object instanceof GeoServerInfo) {
            return GEOSERVER_ID;
        }
        if (object instanceof LoggingInfo) {
            return LOGGING_ID;
        }

        throw new IllegalStateException();
    }

    public static @NonNull ConfigInfoType typeOf(@NonNull Info info) {
        return ConfigInfoType.valueOf(info);
    }
}
