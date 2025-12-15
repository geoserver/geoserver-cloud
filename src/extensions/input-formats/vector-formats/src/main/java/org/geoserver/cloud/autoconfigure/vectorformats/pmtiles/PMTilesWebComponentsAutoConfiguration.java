/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.vectorformats.pmtiles;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.pmtiles.web.data.PMTilesStoreConfiguration;
import org.geotools.autoconfigure.vectorformats.DataAccessFactoryFilteringAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for GeoParquet extension that provides a data store
 * configuration panel for the web admin interface.
 *
 * <p>
 * This auto-configuration class enables the PMTiles extension in GeoServer
 * Cloud. It will be activated when the following conditions are met:
 * <ul>
 * <li>The {@code PMTilesDataStoreFactory} class is on the classpath
 * <li>The {@code geoserver.extension.pmtiles.enabled} property is {@code true} (the default)
 * <li>The {@literal @ConditionalOnGeoServerWebUI} conditional is satisfied
 * </ul>
 *
 * @since 2.28.0
 * @see PMTilesStoreConfiguration
 */
@AutoConfiguration(after = DataAccessFactoryFilteringAutoConfiguration.class)
@ConditionalOnPMTiles
@ConditionalOnGeoServerWebUI
@Import(PMTilesStoreConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.vectorformats.pmtiles")
public class PMTilesWebComponentsAutoConfiguration {

    @PostConstruct
    void log() {
        log.info("PMTiles WebUI extension installed");
    }
}
