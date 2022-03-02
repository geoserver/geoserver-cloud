/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.ConfigInfoInfoType;

import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
public abstract class RemoteRemoveEvent<S, I extends Info> extends RemoteInfoEvent<S, I> {
    private static final long serialVersionUID = 1L;

    protected RemoteRemoveEvent() {
        // default constructor, needed for deserialization
    }

    protected RemoteRemoveEvent(
            @NonNull S source,
            @NonNull String objectId,
            @NonNull ConfigInfoInfoType type,
            @NonNull String originService,
            String destinationService) {
        super(source, objectId, type, originService, destinationService);
    }

    /** Access the removed object value if provided by the remote event publisher */
    public Optional<I> object() {
        return Optional.ofNullable(getObject());
    }

    public abstract I getObject();

    public abstract void setObject(I object);
}
