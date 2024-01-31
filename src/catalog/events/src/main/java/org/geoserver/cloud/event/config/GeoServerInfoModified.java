/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

import org.geoserver.catalog.plugin.Patch;
import org.geoserver.config.GeoServerInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("GeoServerInfoModified")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("serial")
public class GeoServerInfoModified extends ConfigInfoModified implements ConfigInfoEvent {

    protected GeoServerInfoModified() {
        // default constructor, needed for deserialization
    }

    protected GeoServerInfoModified(
            long updateSequence, @NonNull GeoServerInfo info, @NonNull Patch patch) {
        super(updateSequence, resolveId(info), prefixedName(info), typeOf(info), patch);
    }

    public static GeoServerInfoModified createLocal(
            long updateSequence, GeoServerInfo info, @NonNull Patch patch) {

        return new GeoServerInfoModified(updateSequence, info, patch);
    }
}
