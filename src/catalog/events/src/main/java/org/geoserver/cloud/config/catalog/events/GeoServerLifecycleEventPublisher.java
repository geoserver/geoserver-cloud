/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.events;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.event.lifecycle.LifecycleEvent;
import org.geoserver.cloud.event.lifecycle.ReloadEvent;
import org.geoserver.cloud.event.lifecycle.ResetEvent;
import org.geoserver.config.impl.GeoServerLifecycleHandler;

import java.util.function.Consumer;

@RequiredArgsConstructor
@Slf4j
class GeoServerLifecycleEventPublisher implements GeoServerLifecycleHandler {

    private final @NonNull Consumer<? super LifecycleEvent> eventPublisher;

    void publish(@NonNull LifecycleEvent event) {
        eventPublisher.accept(event);
    }

    @Override
    public void onReset() {
        log.debug("Publishing the onReset event");

        publish(new ResetEvent());
    }

    @Override
    public void onDispose() {
        log.debug("Ignoring the onDispose event");
    }

    @Override
    public void beforeReload() {
        // Thus, we want to inform all connected services as early as possible
        // to activate reloading in parallel.
        log.debug("Publishing the beforeReload event");

        publish(new ReloadEvent());
    }

    @Override
    public void onReload() {
        log.debug("Ignoring the onReload event");
    }
}
