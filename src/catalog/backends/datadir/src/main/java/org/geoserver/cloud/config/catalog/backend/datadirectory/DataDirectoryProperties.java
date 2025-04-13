/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.datadirectory;

import java.nio.file.Path;
import java.util.List;
import lombok.Data;
import org.geoserver.cloud.autoconfigure.catalog.backend.datadir.DataDirectoryAutoConfiguration;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to use GeoServer's traditional, file-system based data-directory as the
 * {@link GeoServerBackendConfigurer catalog and configuration backend} through the {@link
 * DataDirectoryAutoConfiguration} auto-configuration.
 */
@ConfigurationProperties(prefix = "geoserver.backend.data-directory")
@Data
public class DataDirectoryProperties {

    private boolean enabled;
    private Path location;
    private boolean parallelLoader = true;
    private DataDirectoryProperties.EventualConsistencyConfig eventualConsistency = new EventualConsistencyConfig();

    /**
     * Eventual consistency enfocement configuration. Bus events may come out of order under stress
     */
    @Data
    public static class EventualConsistencyConfig {
        /**
         * If enabled, the data directory catalog will be resilient to bus events coming out of
         * order
         */
        private boolean enabled = true;

        /**
         * milliseconds to wait before retrying Catalog.getXXX point queries returning null. The
         * list size determines the number of retries. The values the milliseconds to wait
         */
        private List<Integer> retries = List.of(25, 25, 50);
    }
}
