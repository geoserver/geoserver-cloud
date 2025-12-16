/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.vectorformats.graticule;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.configuration.community.graticule.GraticuleWebUIConfiguration;
import org.geotools.autoconfigure.vectorformats.DataAccessFactoryFilteringAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for Graticule extension that provides a data store for
 * graticule lines.
 *
 * <p>
 * This auto-configuration class enables the Graticule extension in GeoServer
 * Cloud, allowing users to create data stores for latitude/longitude graticule
 * lines. It will be activated when the following conditions are met:
 * <ul>
 * <li>The GraticuleDataStoreFactory class is on the classpath</li>
 * </ul>
 *
 * @since 2.27.0
 * @see GraticuleWebUIConfiguration
 */
@AutoConfiguration(after = DataAccessFactoryFilteringAutoConfiguration.class)
@ConditionalOnGraticule
@ConditionalOnGeoServerWebUI
@Import(GraticuleWebUIConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.vectorformats.graticule")
public class GraticuleWebComponentsAutoConfiguration {
    @PostConstruct
    void log() {
        log.info("Graticule WebUI extension enabled");
    }
}
