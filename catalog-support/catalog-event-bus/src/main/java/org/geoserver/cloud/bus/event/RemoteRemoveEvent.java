/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.geoserver.catalog.Info;

@EqualsAndHashCode(callSuper = true)
public abstract class RemoteRemoveEvent<S, I extends Info> extends RemoteInfoEvent<S, I> {
    private static final long serialVersionUID = 1L;

    private @Getter @Setter I object;

    protected RemoteRemoveEvent() {
        // default constructor, needed for deserialization
    }

    protected RemoteRemoveEvent(
            @NonNull S source,
            @NonNull I info,
            @NonNull String originService,
            String destinationService) {
        super(source, info, originService, destinationService);
        this.object = info;
    }

    /** Access the removed object value if provided by the remote event publisher */
    public Optional<I> object() {
        return Optional.ofNullable(getObject());
    }
}
