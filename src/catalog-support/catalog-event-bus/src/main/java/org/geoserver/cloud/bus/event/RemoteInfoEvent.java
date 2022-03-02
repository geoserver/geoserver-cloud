/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.ConfigInfoInfoType;
import org.geoserver.config.GeoServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

@EqualsAndHashCode(callSuper = true)
public abstract class RemoteInfoEvent<S, I extends Info> extends RemoteApplicationEvent {

    private static final long serialVersionUID = 1L;

    private @Autowired @Setter @JsonIgnore transient ServiceMatcher busServiceMatcher;

    protected @JsonIgnore @Autowired @Qualifier("rawCatalog") Catalog catalog;
    protected @JsonIgnore @Autowired @Qualifier("geoServer") GeoServer config;

    /**
     * Identifier of the catalog or config object this event refers to, from {@link Info#getId()}
     */
    private @Getter String objectId;

    private @Getter ConfigInfoInfoType infoType;

    /** Deserialization-time constructor, {@link #getSource()} will be {@code null} */
    protected RemoteInfoEvent() {
        // default constructor, needed for deserialization
    }

    /** Publish-time constructor, {@link #getSource()} won't be {@code null} */
    protected RemoteInfoEvent(
            @NonNull S source,
            @NonNull String objectId,
            @NonNull ConfigInfoInfoType infoType,
            String originService,
            String destinationService) {

        super(source, originService, destinationService);
        this.objectId = objectId;
        this.infoType = infoType;
    }

    public @JsonIgnore boolean isFromSelf() {
        return busServiceMatcher.isFromSelf(this);
    }

    public @Override String toString() {
        return String.format(
                "%s(type: %s, id: %s, event id: %s, from: %s, to: %s, ts: %d]",
                getClass().getSimpleName(),
                getInfoType(),
                this.getObjectId(),
                super.getId(),
                super.getOriginService(),
                super.getDestinationService(),
                super.getTimestamp());
    }
}
