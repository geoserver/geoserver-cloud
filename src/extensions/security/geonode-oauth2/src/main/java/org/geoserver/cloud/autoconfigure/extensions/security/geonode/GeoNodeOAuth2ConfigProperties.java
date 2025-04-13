/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.geonode;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for GeoNode OAuth2 extension.
 *
 * <p>
 * This class defines the configuration properties for the GeoNode OAuth2 extension,
 * with a prefix of "geoserver.extension.security.geonode-oauth2".
 *
 * <p>
 * Available properties:
 * <ul>
 *   <li><b>enabled</b>: Whether the GeoNode OAuth2 extension is enabled (default: true)</li>
 * </ul>
 *
 * <p>
 * Example YAML configuration:
 * <pre>{@code
 * geoserver:
 *   extension:
 *     security:
 *       geonode-oauth2:
 *         enabled: true
 * }</pre>
 *
 * <p>The externalized configuration in config/geoserver.yml provides backward compatibility
 * with the older property through property placeholders:
 *
 * <pre>{@code
 * geoserver:
 *   security:
 *     geonode:
 *       enabled: true
 * }</pre>
 *
 * @since 2.27.0
 */
@ConfigurationProperties(prefix = "geoserver.extension.security.geonode-oauth2")
public @Data class GeoNodeOAuth2ConfigProperties {

    /** Default value for the enabled property (true) */
    static final boolean DEFAULT = true;

    /**
     * Whether the GeoNode OAuth2 extension is enabled.
     * When true, the GeoNode OAuth2 authentication provider will be registered.
     * When false, the extension is disabled.
     */
    private boolean enabled = DEFAULT;
}
