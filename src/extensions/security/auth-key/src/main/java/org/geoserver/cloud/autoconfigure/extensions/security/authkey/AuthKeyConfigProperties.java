/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.security.authkey;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the AuthKey extension.
 *
 * <p>This extension enables authentication via URL parameter or HTTP header tokens in GeoServer.
 *
 * <p>Available properties:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     security:
 *       auth-key:
 *         enabled: true  # Enables the AuthKey extension (default: true)
 * }</pre>
 *
 * <p>When enabled, this extension registers the AuthKey authentication provider in GeoServer.
 *
 * @since 2.27.0
 */
@ConfigurationProperties(prefix = "geoserver.extension.security.auth-key")
public @Data class AuthKeyConfigProperties {

    /** Default value for the enabled property (false). */
    static final boolean DEFAULT = false;

    /**
     * Whether the AuthKey extension is enabled.
     *
     * <p>When set to true, the AuthKey extension components will be registered in the application
     * context. When false, the extension will not be available for use.
     *
     * <p>Default is {@code true}.
     */
    private boolean enabled = DEFAULT;

    /** Prefix for configuration properties. */
    public static final String PREFIX = "geoserver.extension.security.auth-key";
}
