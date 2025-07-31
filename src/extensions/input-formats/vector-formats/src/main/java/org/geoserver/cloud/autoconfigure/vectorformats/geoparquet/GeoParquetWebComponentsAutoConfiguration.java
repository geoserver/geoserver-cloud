/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.vectorformats.geoparquet;

import javax.annotation.PostConstruct;

import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.configuration.community.geoparquet.GeoParquetWebUIConfiguration;
import org.geotools.autoconfigure.vectorformats.DataAccessFactoryFilteringAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

import lombok.extern.slf4j.Slf4j;

/**
 * Auto-configuration for GeoParquet extension that provides a data store
 * configuration panel for the web admin interface.
 *
 * <p>
 * This auto-configuration class enables the GeoParquet extension in GeoServer
 * Cloud. It will be activated when the following conditions are met:
 * <ul>
 * <li>The {@code GeoParquetDataStoreFactory} class is on the classpath</li>
 * <li>The {@literal @ConditionalOnGeoServerWebUI} conditional is satisfied
 * </ul>
 *
 * @since 2.27.0
 * @see GeoParquetWebUIConfiguration
 */
@AutoConfiguration(after = DataAccessFactoryFilteringAutoConfiguration.class)
@ConditionalOnGeoParquet
@ConditionalOnGeoServerWebUI
@Import(GeoParquetWebUIConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.vectorformats.geoparquet")
public class GeoParquetWebComponentsAutoConfiguration {

    @PostConstruct
    void log() {
        log.info("GeoParquet WebUI extension installed");
    }
}
