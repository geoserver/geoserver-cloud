/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.vectorformats;

import lombok.RequiredArgsConstructor;
import org.geoserver.cloud.autoconfigure.catalog.backend.core.GeoServerBackendAutoConfiguration;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration to enable filtering of GeoTools DataAccessFactory
 * implementations.
 *
 * <p>
 * This configuration provides fine-grained control over which data formats are
 * available in the application, allowing selective enabling/disabling of
 * specific factory implementations using their user-friendly display names.
 *
 * <p>
 * Configuration example:
 *
 * <pre>
 * geotools.data.filtering:
 *   enabled: true
 *   vector-formats:
 *     "[PostGIS]": true
 *     "[Oracle NG]": false
 *     "[Shapefile]": true
 *     "[Web Feature Server (NG)]": ${my.wfs.enabled:false}
 * </pre>
 * <p>
 * It works by directly deregistering disabled factories from DataAccessFinder
 * and DataStoreFinder through a BeanPostProcessor that processes the
 * configuration as early as possible in the application startup process.
 */
@AutoConfiguration(before = GeoServerBackendAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@EnableConfigurationProperties(DataAccessFactoryFilterConfigProperties.class)
@ConditionalOnProperty(
        name = DataAccessFactoryFilterConfigProperties.ENABLED_PROP,
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class DataAccessFactoryFilteringAutoConfiguration {

    /**
     * Provides a BeanPostProcessor that will deregister disabled factories.
     *
     * @param configProperties the configuration properties
     * @return the bean post processor
     */
    @Bean
    BeanPostProcessor dataAccessFactoryFilterProcessor(DataAccessFactoryFilterConfigProperties configProperties) {
        return new DataAccessFactoryFilterProcessor(configProperties);
    }
}
