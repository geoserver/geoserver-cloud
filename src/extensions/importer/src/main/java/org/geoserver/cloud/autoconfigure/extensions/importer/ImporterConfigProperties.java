/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.importer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the GeoServer Importer extension.
 */
@Data
@ConfigurationProperties(prefix = ImporterConfigProperties.PREFIX)
public class ImporterConfigProperties {

    public static final String PREFIX = "geoserver.extension.importer";
    public static final boolean DEFAULT_ENABLED = false;

    /**
     * Enable/disable the Importer extension.
     */
    private boolean enabled = DEFAULT_ENABLED;
}
