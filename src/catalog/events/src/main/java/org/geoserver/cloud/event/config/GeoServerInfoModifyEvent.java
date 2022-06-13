/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("GeoServerInfoModified")
@JsonSubTypes({
    @JsonSubTypes.Type(value = UpdateSequenceEvent.class),
})
@EqualsAndHashCode(callSuper = true)
public class GeoServerInfoModifyEvent
        extends ConfigInfoModifyEvent<GeoServerInfoModifyEvent, GeoServerInfo>
        implements ConfigInfoEvent {

    protected GeoServerInfoModifyEvent() {
        // default constructor, needed for deserialization
    }

    protected GeoServerInfoModifyEvent(
            GeoServer source, GeoServer target, @NonNull String id, @NonNull Patch patch) {
        super(source, target, id, ConfigInfoType.GeoServerInfo, patch);
    }

    public static GeoServerInfoModifyEvent createLocal(
            GeoServer source, GeoServerInfo info, @NonNull Patch patch) {
        final String id = resolveId(info);
        return patch.get("updateSequence")
                .map(
                        p ->
                                (GeoServerInfoModifyEvent)
                                        UpdateSequenceEvent.createLocal(source, id, patch))
                .orElseGet(() -> new GeoServerInfoModifyEvent(source, null, id, patch));
    }
}
