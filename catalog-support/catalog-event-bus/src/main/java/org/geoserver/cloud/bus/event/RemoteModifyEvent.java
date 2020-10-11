/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import java.util.Optional;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.plugin.Patch;

@EqualsAndHashCode(callSuper = true)
// @JsonIgnoreProperties(value = {"object", "diff", "payloadCodec"})
public abstract class RemoteModifyEvent<S, I extends Info> extends RemoteInfoEvent<S, I> {
    private static final long serialVersionUID = 1L;

    private @Getter @Setter Patch patch;

    protected RemoteModifyEvent() {
        // default constructor, needed for deserialization
    }

    protected RemoteModifyEvent(
            S source,
            @NonNull I object,
            @NonNull Patch patch,
            String originService,
            String destinationService) {
        super(source, object, originService, destinationService);
        this.patch = patch;
    }

    public @Override String toString() {
        return String.format(
                "%s(type: %s, id: %s, properties: %s, event id: %s, from: %s, to: %s, ts: %d)",
                getClass().getSimpleName(),
                getInfoType(),
                this.getObjectId(),
                patchNamesForLog(),
                super.getId(),
                super.getOriginService(),
                super.getDestinationService(),
                super.getTimestamp());
    }

    private String patchNamesForLog() {
        return patch == null
                ? "<not present>"
                : patch.getPatches()
                        .stream()
                        .map(Patch.Property::getName)
                        .collect(Collectors.joining(","));
    }

    public Optional<Patch> patch() {
        return Optional.ofNullable(getPatch());
    }
}
