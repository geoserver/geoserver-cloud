/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.appschema;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the App-Schema extension.
 *
 * <p>The App-Schema extension enables complex feature mapping using XML-based application schemas
 * in GeoServer. It allows serving complex features by mapping simple features (e.g., from databases)
 * to complex schemas.
 *
 * <p>Available properties:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     appschema:
 *       enabled: true  # Enables the App-Schema extension (default: false)
 * }</pre>
 *
 * <p>When enabled, this extension registers the "Application Schema DataAccess" data store factory
 * and all necessary components for App-Schema support in GeoServer.
 *
 * @since 2.27.0.0
 */
@ConfigurationProperties(prefix = "geoserver.extension.appschema")
public @Data class AppSchemaConfigProperties {

    /** Default value for the enabled property (false). */
    static final boolean DEFAULT = false;

    /**
     * Whether the App-Schema extension is enabled.
     *
     * <p>When set to true, the App-Schema extension components will be registered in the application context.
     * When false, the extension will not be available for use.
     *
     * <p>Default is {@code false}.
     */
    private boolean enabled = DEFAULT;

    /** Prefix for configuration properties. */
    public static final String PREFIX = "geoserver.extension.appschema";
}
