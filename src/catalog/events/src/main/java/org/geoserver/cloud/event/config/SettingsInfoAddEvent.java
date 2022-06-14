/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.EqualsAndHashCode;

import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;

/**
 * Remote event sent when {@link GeoServer#add(org.geoserver.config.SettingsInfo)} is called on a
 * node
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("SettingsInfoAdded")
@EqualsAndHashCode(callSuper = true)
public class SettingsInfoAddEvent extends ConfigInfoAddEvent<SettingsInfoAddEvent, SettingsInfo>
        implements ConfigInfoEvent {

    protected SettingsInfoAddEvent() {
        // default constructor, needed for deserialization
    }

    protected SettingsInfoAddEvent(SettingsInfo object) {
        super(object);
    }

    public static SettingsInfoAddEvent createLocal(SettingsInfo value) {
        return new SettingsInfoAddEvent(value);
    }
}
