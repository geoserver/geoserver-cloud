/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.metrics.catalog;

import io.micrometer.core.instrument.MeterRegistry;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link Catalog} and {@link GeoServer}
 * metrics; depends on the {@literal geoserver.metrics.enabled=true} configuration property.
 *
 * @see CatalogMetrics
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@EnableConfigurationProperties(GeoSeverMetricsConfigProperties.class)
public class CatalogMetricsAutoConfiguration {

    public @Bean CatalogMetrics geoserverCatalogMetrics( //
            GeoSeverMetricsConfigProperties metricsConfig, //
            @Qualifier("catalog") Catalog catalog, //
            @Qualifier("geoServer") GeoServer config) {

        return new CatalogMetrics(metricsConfig, catalog, config);
    }
}
