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
@JsonTypeName("SettingsAdded")
@EqualsAndHashCode(callSuper = true)
public class SettingsAdded extends ConfigInfoAdded<SettingsAdded, SettingsInfo>
        implements ConfigInfoEvent {

    protected SettingsAdded() {
        // default constructor, needed for deserialization
    }

    protected SettingsAdded(long updateSequence, SettingsInfo object) {
        super(updateSequence, object);
    }

    public static SettingsAdded createLocal(long updateSequence, SettingsInfo value) {
        return new SettingsAdded(updateSequence, value);
    }
}
