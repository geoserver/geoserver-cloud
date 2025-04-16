/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.features;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the OGC API Features extension.
 */
@Data
@ConfigurationProperties(prefix = OgcApiFeatureConfigProperties.PREFIX)
public class OgcApiFeatureConfigProperties {

    public static final String PREFIX = "geoserver.extension.ogcapi.features";
    public static final boolean DEFAULT_ENABLED = true;

    /**
     * Whether the OGC API Features extension is enabled (default: true).
     */
    private boolean enabled = DEFAULT_ENABLED;
}
