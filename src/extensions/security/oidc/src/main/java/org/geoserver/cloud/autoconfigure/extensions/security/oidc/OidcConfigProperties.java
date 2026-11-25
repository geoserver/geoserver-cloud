/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.oidc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the OIDC security extension.
 *
 * <p>Available properties:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     security:
 *       oidc:
 *         enabled: true  # Enables the OIDC extension (default: true)
 * }</pre>
 *
 * <p>When enabled, the OIDC authentication provider becomes available to configure from the GeoServer admin UI. The
 * per-provider settings (client id/secret, authorization and token URIs, etc.) are stored in the standard GeoServer
 * security filter config and are not managed here.
 *
 * @since 3.0.3
 */
@ConfigurationProperties(prefix = OidcConfigProperties.PREFIX)
public @Data class OidcConfigProperties {

    /** Prefix for configuration properties. */
    public static final String PREFIX = "geoserver.extension.security.oidc";

    /** Enabled by default: nobody authenticates through OIDC until an administrator configures a filter. */
    static final boolean DEFAULT = true;

    /**
     * Whether the OIDC extension is enabled.
     *
     * <p>When {@code true}, the OAuth2/OIDC authentication provider and (in the web UI service) its admin panel are
     * registered in the application context so an operator can configure it from the GeoServer admin UI. When
     * {@code false}, the extension is inert.
     *
     * <p>Default is {@code true}.
     */
    private boolean enabled = DEFAULT;
}
