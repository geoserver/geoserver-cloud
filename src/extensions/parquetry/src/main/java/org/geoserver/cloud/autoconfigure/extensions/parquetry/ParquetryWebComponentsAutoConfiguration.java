/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.parquetry;

import io.tileverse.parquetry.geoserver.config.GeoParquetConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Contributes the GeoParquet datastore edit panel to the web admin interface.
 *
 * <p>Imports only {@link GeoParquetConfiguration}. The plugin's Iceberg and Stac-GeoParquet panel configurations stay
 * out until those stores are production ready; their factories are unavailable anyway, see
 * {@link ParquetryContextInitializer}.
 *
 * @since 3.1.0
 */
@AutoConfiguration
@ConditionalOnParquetry
@ConditionalOnGeoServerWebUI
@Import(GeoParquetConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.parquetry")
public class ParquetryWebComponentsAutoConfiguration {

    @PostConstruct
    void log() {
        log.info("Parquetry WebUI components installed");
    }
}
