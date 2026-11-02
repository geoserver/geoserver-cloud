/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.tileverse.parquetry;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * Contributes the GeoParquet datastore edit panel to the web admin interface, when the "Parquet" datastore feature is
 * on.
 *
 * <p>Imports only the GeoParquet panel and its module status from the plugin's {@code applicationContext.xml}. The
 * plugin's Iceberg and Stac-GeoParquet panels stay out until those stores are production ready; their factories are
 * unavailable anyway, see {@link ParquetryContextInitializer}.
 *
 * <p>The plugin declares plain XML beans on this series, and this extension must import them through XML. The plugin's
 * classes are Java 25 class files, and Spring 5.3 cannot parse them. A {@code @Configuration} class of the plugin would
 * register as an ordinary bean, and Spring would never call its {@code @Bean} methods.
 *
 * @since 2.28.7
 */
@AutoConfiguration
@ConditionalOnParquetryParquet
@ConditionalOnGeoServerWebUI
@ImportFilteredResource(ParquetryWebComponentsAutoConfiguration.GEOPARQUET_PANEL_BEANS)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.tileverse.parquetry")
public class ParquetryWebComponentsAutoConfiguration {

    static final String GEOPARQUET_PANEL_BEANS =
            "jar:tileverse-geoserver-parquetry-.*!/applicationContext.xml#name=^(parquetryGeoParquetDataStorePanel|geoParquetModuleStatus)$";

    @PostConstruct
    void log() {
        log.info("Parquetry WebUI components installed");
    }
}
