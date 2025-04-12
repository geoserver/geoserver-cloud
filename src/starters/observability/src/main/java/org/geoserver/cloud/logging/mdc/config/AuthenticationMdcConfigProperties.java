/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.logging.mdc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for controlling which authentication information is included in the MDC.
 * <p>
 * These properties determine what user-related information is added to the MDC (Mapped Diagnostic Context)
 * during request processing. Including this information in the MDC makes it available to all logging
 * statements, providing valuable context for audit, security, and debugging purposes.
 * <p>
 * The properties are configured using the prefix {@code logging.mdc.include.user} in the application
 * properties or YAML files.
 * <p>
 * Example configuration in YAML:
 * <pre>
 * logging:
 *   mdc:
 *     include:
 *       user:
 *         id: true
 *         roles: true
 * </pre>
 *
 * @see org.geoserver.cloud.logging.mdc.servlet.MDCAuthenticationFilter
 * @see org.geoserver.cloud.logging.mdc.webflux.MDCWebFilter
 */
@Data
@ConfigurationProperties(prefix = "logging.mdc.include.user")
public class AuthenticationMdcConfigProperties {

    /** Whether to append the enduser.id MDC property from the Authentication name */
    private boolean id = false;

    /**
     * Whether to append the enduser.roles MDC property from the Authentication granted authorities
     */
    private boolean roles = false;
}
