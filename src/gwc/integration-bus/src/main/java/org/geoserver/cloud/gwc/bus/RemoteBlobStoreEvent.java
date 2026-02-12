/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.bus;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @since 1.0
 */
@EqualsAndHashCode(callSuper = true)
public class RemoteBlobStoreEvent extends RemoteGeoWebCacheEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private @Getter @Setter String blobStoreId;
    private @Getter @Setter String oldName;

    @JsonCreator
    protected RemoteBlobStoreEvent() {
        // default constructor, needed for deserialization
    }

    public RemoteBlobStoreEvent(Object source, @NonNull String originService) {
        super(source, originService);
    }

    public RemoteBlobStoreEvent(
            Object source, @NonNull String originService, @NonNull String blobStoreId, @NonNull Type eventType) {
        super(source, originService, eventType);
        this.blobStoreId = blobStoreId;
    }

    protected @Override String getObjectId() {
        return blobStoreId;
    }
}
