/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the INSPIRE extension.
 *
 * <p>Available properties:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     inspire:
 *       enabled: true  # Enables the INSPIRE extension (default: false)
 * }</pre>
 *
 * @since 2.27.0.0
 */
@ConfigurationProperties(prefix = InspireConfigProperties.PREFIX)
public @Data class InspireConfigProperties {

    /** Prefix for configuration properties. */
    public static final String PREFIX = "geoserver.extension.inspire";

    /** Default value for the enabled property (false). */
    static final boolean DEFAULT = false;

    /**
     * Whether the INSPIRE extension is enabled.
     *
     * <p>When set to true, the INSPIRE extension components will be registered in the application context.
     * When false, the extension will not be available for use.
     *
     * <p>Default is {@code false}.
     */
    private boolean enabled = DEFAULT;
}
