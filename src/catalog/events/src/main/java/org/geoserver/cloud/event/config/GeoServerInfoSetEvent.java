/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;

/** Event sent when {@link GeoServer#setGlobal(GeoServerInfo)} is called */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("GeoServerInfoSet")
public class GeoServerInfoSetEvent extends ConfigInfoAddEvent<GeoServerInfoSetEvent, GeoServerInfo>
        implements ConfigInfoEvent {

    protected GeoServerInfoSetEvent() {
        // default constructor, needed for deserialization
    }

    protected GeoServerInfoSetEvent(GeoServerInfo object) {
        super(object);
    }

    public static GeoServerInfoSetEvent createLocal(GeoServerInfo value) {
        return new GeoServerInfoSetEvent(value);
    }
}
