/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.lifecycle;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.geoserver.cloud.event.GeoServerEvent;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ReloadEvent.class),
    @JsonSubTypes.Type(value = ResetEvent.class)
})
@SuppressWarnings("serial")
public abstract class LifecycleEvent extends GeoServerEvent {

    @Override
    public String toShortString() {
        String originService = getOrigin();
        String type = getClass().getSimpleName();
        return "%s[origin: %s]".formatted(type, originService);
    }
}
