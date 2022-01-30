/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.bus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/** @since 1.0 */
@NoArgsConstructor
public class RemoteBlobStoreEvent extends RemoteGeoWebCacheEvent {

    private static final long serialVersionUID = 1L;

    private @Getter @Setter String blobStoreId;

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

    public @Override String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), getBlobStoreId());
    }
}
