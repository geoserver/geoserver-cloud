/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.security.environmentadmin;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Environment Admin Authentication extension.
 *
 * <p>
 * This class defines the configuration properties for the Environment Admin Authentication extension,
 * with a prefix of "geoserver.extension.security.environment-admin".
 *
 * <p>
 * Available properties:
 * <ul>
 *   <li><b>enabled</b>: Whether the Environment Admin Authentication extension is enabled (default: true)</li>
 * </ul>
 *
 * <p>
 * Example YAML configuration:
 * <pre>{@code
 * geoserver:
 *   extension:
 *     security:
 *       environment-admin:
 *         enabled: true
 * }</pre>
 *
 * <p>
 * This extension also requires the core admin username and password properties to be set:
 * <pre>{@code
 * geoserver:
 *   admin:
 *     username: admin
 *     password: geoserver
 * }</pre>
 *
 * @since 2.27.0
 */
@ConfigurationProperties(prefix = EnvironmentAdminConfigProperties.PREFIX)
public @Data class EnvironmentAdminConfigProperties {

    /** Default enabled value is true */
    public static final boolean DEFAULT_ENABLED = true;

    /** Configuration prefix */
    public static final String PREFIX = "geoserver.extension.security.environment-admin";

    /** Whether the Environment Admin Authentication extension is enabled */
    private boolean enabled = DEFAULT_ENABLED;
}
