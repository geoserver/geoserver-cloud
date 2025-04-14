/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.autoconfigure.rasterformats;

import lombok.RequiredArgsConstructor;
import org.geoserver.cloud.autoconfigure.catalog.backend.core.GeoServerBackendAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration to enable filtering of GeoTools GridFormatFactorySpi
 * implementations.
 *
 * <p>
 * This configuration provides fine-grained control over which raster formats are
 * available in the application, allowing selective enabling/disabling of
 * specific factory implementations using their user-friendly display names.
 *
 * <p>
 * Configuration example:
 *
 * <pre>
 * geotools.data.filtering:
 *   enabled: true
 *   raster-formats:
 *     "[ArcGrid]": true
 *     "[GeoTIFF]": true
 *     "[ImageMosaic]": false
 *     "[WorldImage]": true
 * </pre>
 * <p>
 * It works by using reflection to access GridFormatFinder's internal FactoryRegistry
 * and remove disabled factories. This is done through a BeanPostProcessor that
 * processes the configuration as early as possible in the application startup process.
 */
@AutoConfiguration(before = GeoServerBackendAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@EnableConfigurationProperties(GridFormatFactoryFilterConfigProperties.class)
@ConditionalOnProperty(
        name = GridFormatFactoryFilterConfigProperties.ENABLED_PROP,
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class GridFormatFactoryFilteringAutoConfiguration {

    /**
     * Provides a GridFormatFactoryFilterProcessor that will deregister disabled factories.
     *
     * @param configProperties the configuration properties
     * @return the bean post processor
     */
    @Bean
    GridFormatFactoryFilterProcessor gridFormatFactoryFilterProcessor(
            GridFormatFactoryFilterConfigProperties configProperties) {
        return new GridFormatFactoryFilterProcessor(configProperties);
    }
}
