/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.ldap;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the LDAP security extension.
 *
 * <p>The LDAP security extension enables user authentication, user groups and roles to be stored
 * and managed in an LDAP directory.
 *
 * <p>Available properties:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     security:
 *       ldap:
 *         enabled: true  # Enables the LDAP security extension (default: true)
 * }</pre>
 *
 * <p>This configuration provides backward compatibility with the older property:
 * {@code geoserver.security.ldap=true|false}
 *
 * @since 2.27.0.0
 */
@ConfigurationProperties(prefix = LDAPConfigProperties.PREFIX)
public @Data class LDAPConfigProperties {

    /** Default value for the enabled property (true). */
    static final boolean DEFAULT = true;

    /**
     * Whether the LDAP security extension is enabled.
     *
     * <p>When set to true, the LDAP security extension components will be registered in the application context.
     * When false, the extension will not be available for use.
     *
     * <p>Default is {@code true}.
     */
    private boolean enabled = DEFAULT;

    /** Prefix for configuration properties. */
    public static final String PREFIX = "geoserver.extension.security.ldap";
}
