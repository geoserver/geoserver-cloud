/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import org.geoserver.catalog.plugin.Patch;
import org.geoserver.config.GeoServerInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("UpdateSequence")
@EqualsAndHashCode(callSuper = true)
public class UpdateSequenceEvent extends GeoServerInfoModifyEvent {

    protected UpdateSequenceEvent() {
        // default constructor, needed for deserialization
    }

    /**
     * The provided {@link GeoServerInfo}'s {@link GeoServerInfo#getUpdateSequence() update
     * sequence}. Being the most frequently updated property, it's readily available for remote
     * listeners even when the {@link #getPatch() patch} is not sent over the wire.
     */
    private @Getter long updateSequence;

    protected UpdateSequenceEvent(@NonNull String id, @NonNull Patch patch, long updateSequence) {
        super(id, patch);
        this.updateSequence = updateSequence;
    }

    public @Override String toString() {
        return toStringBuilder().append("updateSequence", getUpdateSequence()).toString();
    }

    public static UpdateSequenceEvent createLocal(GeoServerInfo info) {

        long updateSequence = info.getUpdateSequence();
        String id = resolveId(info);
        Patch patch = new Patch();
        patch.add("updateSequence", updateSequence);
        return createLocal(id, patch);
    }

    public static UpdateSequenceEvent createLocal(@NonNull String id, @NonNull Patch patch) {

        long updateSequence = (long) patch.get("updateSequence").orElseThrow().getValue();
        return new UpdateSequenceEvent(id, patch, updateSequence);
    }
}
