/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.ConfigInfoInfoType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
// @JsonIgnoreProperties(value = {"object", "diff", "payloadCodec"})
public abstract class RemoteModifyEvent<S, I extends Info> extends RemoteInfoEvent<S, I> {
    private static final long serialVersionUID = 1L;

    private @Getter @Setter List<String> changedProperties;
    private @Getter @Setter Patch patch;

    protected RemoteModifyEvent() {
        // default constructor, needed for deserialization
    }

    protected RemoteModifyEvent(
            S source,
            @NonNull String objectId,
            @NonNull ConfigInfoInfoType type,
            @NonNull Patch patch,
            String originService,
            String destinationService) {
        super(source, objectId, type, originService, destinationService);
        this.patch = patch;
        this.changedProperties = patch.getPropertyNames();
    }

    public @Override String toString() {
        return String.format(
                "%s(type: %s, id: %s, properties: %s, event id: %s, from: %s, to: %s, ts: %d)",
                getClass().getSimpleName(),
                getInfoType(),
                this.getObjectId(),
                patchNamesToString(),
                super.getId(),
                super.getOriginService(),
                super.getDestinationService(),
                super.getTimestamp());
    }

    private String patchNamesToString() {
        return changedProperties == null
                ? "<not present>"
                : changedProperties.stream().collect(Collectors.joining(","));
    }

    public Optional<Patch> patch() {
        return Optional.ofNullable(getPatch());
    }
}
