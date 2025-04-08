/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.security.jdbc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the JDBC security extension.
 *
 * <p>The JDBC security extension enables user authentication, user groups and roles to be stored
 * and managed in a database through JDBC.
 *
 * <p>Available properties:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     security:
 *       jdbc:
 *         enabled: true  # Enables the JDBC security extension (default: true)
 * }</pre>
 *
 * <p>This configuration provides backward compatibility with the older property:
 * {@code geoserver.security.jdbc=true|false}
 *
 * @since 2.27.0.0
 */
@ConfigurationProperties(prefix = JDBCConfigProperties.PREFIX)
public @Data class JDBCConfigProperties {

    /** Default value for the enabled property (true). */
    static final boolean DEFAULT = true;

    /**
     * Whether the JDBC security extension is enabled.
     *
     * <p>When set to true, the JDBC security extension components will be registered in the application context.
     * When false, the extension will not be available for use.
     *
     * <p>Default is {@code true}.
     */
    private boolean enabled = DEFAULT;

    /** Prefix for configuration properties. */
    public static final String PREFIX = "geoserver.extension.security.jdbc";
}
