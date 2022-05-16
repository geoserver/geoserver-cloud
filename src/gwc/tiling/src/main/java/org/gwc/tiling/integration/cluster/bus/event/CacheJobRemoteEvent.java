/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.integration.cluster.bus.event;

import lombok.Getter;
import lombok.NonNull;

import org.gwc.tiling.cluster.event.CacheJobEvent;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

/**
 * @since 1.0
 */
public abstract class CacheJobRemoteEvent<L extends CacheJobEvent> extends RemoteApplicationEvent {
    private static final long serialVersionUID = 1L;

    private @Getter L event;

    protected CacheJobRemoteEvent() {
        super();
    }

    public CacheJobRemoteEvent(
            @NonNull Object source,
            @NonNull String originService,
            @NonNull Destination destination) {
        super(source, originService, destination);
    }

    @SuppressWarnings("unchecked")
    public <T extends CacheJobRemoteEvent<?>> T setEvent(L localEvent) {
        this.event = localEvent;
        return (T) this;
    }
}
