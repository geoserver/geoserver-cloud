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
public class RemoteGridsetEvent extends RemoteGeoWebCacheEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private @Getter @Setter String gridsetId;

    @JsonCreator
    protected RemoteGridsetEvent() {
        // default constructor, needed for deserialization
    }

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
