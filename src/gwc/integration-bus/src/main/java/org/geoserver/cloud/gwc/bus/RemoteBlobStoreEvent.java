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
public class RemoteBlobStoreEvent extends RemoteGeoWebCacheEvent {

    private static final long serialVersionUID = 1L;

    private @Getter @Setter String blobStoreId;
    private @Getter @Setter String oldName;

    public RemoteBlobStoreEvent(Object source, @NonNull String originService) {
        super(source, originService);
    }

    public RemoteBlobStoreEvent(
            Object source,
            @NonNull String originService,
            @NonNull String blobStoreId,
            @NonNull Type eventType) {
        super(source, originService, eventType);
        this.blobStoreId = blobStoreId;
    }

    protected @Override String getObjectId() {
        return blobStoreId;
    }
}
