/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.gateway.sharedauth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Gateway Shared Authentication extension.
 *
 * <p>This extension enables sharing authentication between the GeoServer Web UI and other
 * microservices through the gateway.
 *
 * <p>Available properties:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     security:
 *       gateway-shared-auth:
 *         enabled: true  # Enable/disable Gateway Shared Authentication (default: true)
 *         auto: true     # Automatically configure auth filter chains (default: true)
 * }</pre>
 *
 * <p>This configuration provides backward compatibility with the older properties:
 * <pre>{@code
 * geoserver.extension.security.gateway-shared-auth.enabled=true|false
 * geoserver.extension.security.gateway-shared-auth.auto=true|false
 * }</pre>
 *
 * @since 2.27.0.0
 */
@ConfigurationProperties(prefix = GatewaySharedAuthConfigProperties.PREFIX)
public @Data class GatewaySharedAuthConfigProperties {

    /** Default value for the enabled property (true). */
    static final boolean DEFAULT_ENABLED = true;

    /** Default value for the auto property (true). */
    static final boolean DEFAULT_AUTO = true;

    /**
     * Whether the Gateway Shared Authentication extension is enabled.
     *
     * <p>When enabled, authentication can be shared between microservices through the gateway.
     * The same configuration must be applied to the gateway service.
     *
     * <p>Default is {@code true}.
     */
    private boolean enabled = DEFAULT_ENABLED;

    /**
     * Whether to automatically create the gateway-shared-auth authentication filter.
     *
     * <p>When true, the filter will be automatically created and appended to the filter chains.
     *
     * <p>Default is {@code true}.
     */
    private boolean auto = DEFAULT_AUTO;

    /** Prefix for configuration properties. */
    public static final String PREFIX = "geoserver.extension.security.gateway-shared-auth";

    static final String ENABLED_PROP = PREFIX + ".enabled";
    static final String AUTO_PROP = PREFIX + ".auto";
}
