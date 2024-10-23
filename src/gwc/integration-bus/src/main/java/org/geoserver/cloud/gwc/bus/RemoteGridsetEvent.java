/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.bus;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * @since 1.0
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RemoteGridsetEvent extends RemoteGeoWebCacheEvent {

    private static final long serialVersionUID = 1L;

    private @Getter @Setter String gridsetId;

    public RemoteGridsetEvent(Object source, @NonNull String originService) {
        super(source, originService);
    }

    public RemoteGridsetEvent(
            Object source, @NonNull String originService, @NonNull String gridsetId, @NonNull Type eventType) {
        super(source, originService, eventType);
        this.gridsetId = gridsetId;
    }

    protected @Override String getObjectId() {
        return gridsetId;
    }
}
