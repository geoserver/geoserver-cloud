/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.geoserver.catalog.Info;

@EqualsAndHashCode(callSuper = true)
public abstract class RemoteAddEvent<S, I extends Info> extends RemoteInfoEvent<S, I> {
    private static final long serialVersionUID = 1L;

    protected RemoteAddEvent() {
        // default constructor, needed for deserialization
    }

    protected RemoteAddEvent(
            @NonNull S source,
            @NonNull I object,
            @NonNull String originService,
            String destinationService) {
        super(source, object, originService, destinationService);
    }
}
