/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.event.bus;

import com.google.common.base.Supplier;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.cloud.event.info.InfoEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Highest precedence listener on {@link InfoEvent}s to set their {@link InfoEvent#setOrigin origin}
 * property
 */
@RequiredArgsConstructor
class LocalInfoEventOriginSetter {

    private final @NonNull Supplier<String> serviceId;

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener(InfoEvent.class)
    public void handleLocalEvent(InfoEvent<? extends InfoEvent<?, ?>, ?> event) {
        String originService = serviceId.get();
        event.setOrigin(originService);
    }
}
