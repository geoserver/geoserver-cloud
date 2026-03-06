/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.bus;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.geoserver.cloud.gwc.event.ConfigChangeEvent;

/** @since 1.9 */
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("serial")
public class RemoteConfigChangeEvent extends RemoteGeoWebCacheEvent {

    @JsonCreator
    protected RemoteConfigChangeEvent() {
        // default constructor, needed for deserialization
    }

    public RemoteConfigChangeEvent(Object source, @NonNull String originService) {
        super(source, originService);
    }

    protected @Override String getObjectId() {
        return ConfigChangeEvent.OBJECT_ID;
    }
}
