/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.metrics.catalog;

import io.micrometer.core.annotation.Timed;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.config.UpdateSequence;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link Catalog} and {@link GeoServer}
 * metrics; depends on the {@literal geoserver.metrics.enabled=true} configuration property.
 *
 * @see CatalogMetrics
 * @since 1.0
 */
@AutoConfiguration(after = {MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@ConditionalOnClass(Timed.class)
@ConditionalOnGeoServerMetricsEnabled
@EnableConfigurationProperties(GeoSeverMetricsConfigProperties.class)
public class CatalogMetricsAutoConfiguration {

    @Bean
    CatalogMetrics geoserverCatalogMetrics( //
            GeoSeverMetricsConfigProperties metricsConfig, //
            @Qualifier("catalog") Catalog catalog, //
            @Qualifier("geoServer") GeoServer config,
            @NonNull UpdateSequence updateSequence) {

        return new CatalogMetrics(metricsConfig, catalog, config, updateSequence);
    }
}
