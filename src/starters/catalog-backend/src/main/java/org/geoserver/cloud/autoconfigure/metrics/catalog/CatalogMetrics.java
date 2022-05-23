/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.metrics.catalog;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;

import java.util.function.Supplier;

/**
 * Registers GeoServer {@link Catalog catalog} and {@link GeoServer config} metrics to be exported
 * by micrometer's {@link MeterRegistry}.
 *
 * <p>The following metrics are exported:
 *
 * <ul>
 *   <li>{@literal geoserver.config.update_sequence}: the configuration {@link
 *       GeoServerInfo#getUpdateSequence() update sequence}
 *   <li>{@literal geoserver.catalog.added}: Number of CatalogInfo objects added to this instance's
 *       Catalog
 *   <li>{@literal geoserver.catalog.removed}: Number of CatalogInfo objects removed on this
 *       instance's Catalog
 *   <li>{@literal geoserver.catalog.modified}: Number of modifications to CatalogInfo objects on
 *       this instance's Catalog
 * </ul>
 *
 * <p>All metrics are tagged with the {@literal instance-id} key, whose value is resolved through
 * <code>${geoserver.metrics.instance-id}</code>, which usually should be the same as <code>
 * ${info.instance-id}</code>.
 *
 * @since 1.0
 */
@RequiredArgsConstructor
@Slf4j(topic = "org.geoserver.cloud.metrics.catalog")
public class CatalogMetrics implements MeterBinder {

    private final @NonNull GeoSeverMetricsConfigProperties metricsConfig;
    private final @NonNull Catalog catalog;
    private final @NonNull GeoServer config;

    private MetricsCatalogListener listener;

    public @Override void bindTo(@NonNull MeterRegistry registry) {
        if (listener != null) {
            catalog.removeListener(listener);
        }

        if (metricsConfig.isEnabled()) {
            final String instanceIdTag = metricsConfig.getInstanceId();
            catalog.addListener(listener = new MetricsCatalogListener(registry, instanceIdTag));

            registerUpdateSequence(registry, instanceIdTag);

            log.info("GeoServer Catalog and config metrics enabled.");
        }
    }

    private void registerUpdateSequence(MeterRegistry registry, final String instanceIdTag) {
        Supplier<Number> updateSequence = () -> config.getGlobal().getUpdateSequence();
        Gauge.Builder<Supplier<Number>> updateSeqBuilder =
                Gauge.builder("geoserver.config.update_sequence", updateSequence)
                        .description("GeoServer configuration update sequence")
                        .baseUnit("sequence");

        if (null != instanceIdTag)
            updateSeqBuilder = updateSeqBuilder.tag("instance-id", instanceIdTag);
        updateSeqBuilder.register(registry);
    }
}
