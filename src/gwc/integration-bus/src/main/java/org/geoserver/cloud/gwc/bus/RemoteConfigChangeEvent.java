/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.bus;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.geoserver.cloud.gwc.event.ConfigChangeEvent;

/**
 * @since 1.9
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("serial")
public class RemoteConfigChangeEvent extends RemoteGeoWebCacheEvent {

    public RemoteConfigChangeEvent(Object source, @NonNull String originService) {
        super(source, originService);
    }

    protected @Override String getObjectId() {
        return ConfigChangeEvent.OBJECT_ID;
    }
}
