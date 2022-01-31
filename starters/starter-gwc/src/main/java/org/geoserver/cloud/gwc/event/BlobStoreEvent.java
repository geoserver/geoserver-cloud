/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.event;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/** @since 1.0 */
public class BlobStoreEvent extends GeoWebCacheEvent {
    private static final long serialVersionUID = 1L;

    private @Getter @Setter String blobStoreId;

    public BlobStoreEvent(Object source) {
        super(source);
    }

    public BlobStoreEvent(Object source, @NonNull Type eventType, @NonNull String blobStoreId) {
        super(source, eventType);
        this.blobStoreId = blobStoreId;
    }

    protected @Override String getObjectId() {
        return blobStoreId;
    }
}
