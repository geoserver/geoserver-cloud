/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.metrics.catalog;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Counter.Builder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.BaseUnits;

import lombok.NonNull;

import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;

import javax.annotation.Nullable;

class MetricsCatalogListener implements CatalogListener {

    private final Counter added;
    private final Counter removed;
    private final Counter modified;
    private final Counter reloads;

    public MetricsCatalogListener(@NonNull MeterRegistry registry, @Nullable String instanceId) {

        added =
                counter("geoserver.catalog.added", instanceId, registry)
                        .description(
                                "Number of CatalogInfo objects added to this instance's Catalog")
                        .register(registry);
        removed =
                counter("geoserver.catalog.removed", instanceId, registry)
                        .description(
                                "Number of CatalogInfo objects removed on this instance's Catalog")
                        .register(registry);
        modified =
                counter("geoserver.catalog.modified", instanceId, registry)
                        .description(
                                "Number of modifications to CatalogInfo objects on this instance's Catalog")
                        .register(registry);

        reloads =
                Counter.builder("geoserver.catalog.reloads")
                        .description("Times the Catalog has been reloaded")
                        .baseUnit(BaseUnits.OPERATIONS)
                        .register(registry);
    }

    private Counter.Builder counter(String name, String instanceId, MeterRegistry registry) {
        Builder builder =
                Counter.builder(name) //
                        .baseUnit(BaseUnits.OPERATIONS);
        if (null != instanceId) builder = builder.tag("instance-id", instanceId);
        return builder;
    }

    public @Override void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        added.increment();
    }

    public @Override void handleRemoveEvent(CatalogRemoveEvent event) {
        removed.increment();
    }

    public @Override void reloaded() {
        reloads.increment();
    }

    public @Override void handlePostModifyEvent(CatalogPostModifyEvent event) {
        modified.increment();
    }

    public @Override void handleModifyEvent(CatalogModifyEvent event) {
        // no-op, see #handlePostModifyEvent
    }
}
